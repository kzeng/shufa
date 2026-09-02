#!/usr/bin/env python3
"""Crawl Yac8 mobile calligraphy categories and merge them into posts.json.

The site is GBK encoded and uses two levels of pagination:
category pages (list_*.html) and article pages (*_2.html, ...).
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import tempfile
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from shutil import which
from pathlib import Path
from typing import Iterable
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen

from bs4 import BeautifulSoup

BASE_URL = "http://www.yac8.com/wap/"
OUTPUT = Path("app/src/main/assets/posts.json")
PROGRESS = Path("scripts/progress/yac8_posts.json")
HEADERS = {"User-Agent": "Mozilla/5.0 (Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"}
COOKIE_HEADER = ""
COOKIE_LOCK = threading.Lock()
BROWSER_LOCAL = threading.local()
USE_BROWSER = True

CATEGORIES = {
    "kaishu": ("楷书", "news/list_97.html", "KAISHU"),
    "xingshu": ("行书", "news/list_141.html", "XINGSHU"),
    "lishu": ("隶书", "news/list_143.html", "LISHU"),
    "caoshu": ("草书", "news/list_142.html", "CAOSHU"),
    "zhuanshu": ("篆书", "news/list_144.html", "ZHUANSHU"),
}


def fetch(url: str, retries: int = 3) -> str:
    if USE_BROWSER:
        return browser_fetch(url)
    last_error: Exception | None = None
    for attempt in range(retries):
        try:
            headers = dict(HEADERS)
            if COOKIE_HEADER:
                headers["Cookie"] = COOKIE_HEADER
            request = Request(url, headers=headers)
            with urlopen(request, timeout=30) as response:
                body = response.read().decode("gbk", errors="replace")
                if "document.cookie" in body and "__tst_status" in body:
                    raise RuntimeError("Yac8 anti-bot challenge returned")
                return body
        except Exception as error:
            last_error = error
            if "anti-bot challenge" in str(error):
                with COOKIE_LOCK:
                    bootstrap_cookies()
            if attempt + 1 < retries:
                time.sleep(1.5 * (attempt + 1))
    try:
        return browser_fetch(url)
    except Exception as browser_error:
        raise RuntimeError(f"failed to fetch {url}: {last_error}; browser fallback: {browser_error}") from browser_error


def bootstrap_cookies() -> None:
    """Use a real browser once so the site's JavaScript challenge can run."""
    global COOKIE_HEADER
    try:
        from playwright.sync_api import sync_playwright

        with sync_playwright() as playwright:
            executable = which("google-chrome") or which("chromium")
            browser = playwright.chromium.launch(
                headless=True,
                executable_path=executable,
                args=["--no-sandbox"] if executable else None,
            )
            context = browser.new_context(user_agent=HEADERS["User-Agent"])
            page = context.new_page()
            page.goto(absolute("news/list_97.html", BASE_URL), wait_until="domcontentloaded", timeout=30000)
            page.wait_for_selector("section.area_newsList", timeout=15000)
            cookies = context.cookies("http://www.yac8.com")
            COOKIE_HEADER = "; ".join(f"{cookie['name']}={cookie['value']}" for cookie in cookies)
            browser.close()
            print(f"Browser bootstrap complete ({len(cookies)} cookies)", flush=True)
    except Exception as error:
        print(f"Browser bootstrap skipped: {error}", flush=True)


def browser_fetch(url: str) -> str:
    """Fetch through Chrome when Yac8 serves its JavaScript challenge."""
    from playwright.sync_api import sync_playwright

    if not hasattr(BROWSER_LOCAL, "page"):
        playwright = sync_playwright().start()
        executable = which("google-chrome") or which("chromium")
        browser = playwright.chromium.launch(
            headless=True,
            executable_path=executable,
            args=["--no-sandbox"] if executable else None,
        )
        context = browser.new_context(user_agent=HEADERS["User-Agent"])
        BROWSER_LOCAL.playwright = playwright
        BROWSER_LOCAL.browser = browser
        BROWSER_LOCAL.page = context.new_page()
    page = BROWSER_LOCAL.page
    page.goto(url, wait_until="domcontentloaded", timeout=45000)
    try:
        page.wait_for_selector("#newsContent", state="attached", timeout=8000)
    except Exception:
        page.wait_for_timeout(1500)
    body = page.content()
    if len(body) < 5000:
        page.reload(wait_until="domcontentloaded", timeout=45000)
        page.wait_for_timeout(1800)
        body = page.content()
    if "document.cookie" in body and "__tst_status" in body:
        raise RuntimeError("browser could not pass Yac8 anti-bot challenge")
    return body


def absolute(href: str, base: str) -> str:
    return urljoin(base, href.replace("\\", ""))


def article_links(html: str, page_url: str) -> list[str]:
    soup = BeautifulSoup(html, "lxml")
    links: list[str] = []
    for link in soup.select("section.area_newsList a[href]"):
        href = absolute(link["href"], page_url)
        if re.search(r"/wap/news/\d+\.html$", urlparse(href).path) and href not in links:
            links.append(href)
    return links


def next_link(html: str, page_url: str, title: str = "下一页") -> str | None:
    soup = BeautifulSoup(html, "lxml")
    for link in soup.select("a[href]"):
        if link.get("title") == title or link.get_text(" ", strip=True) == title:
            href = link.get("href", "")
            if href and not href.startswith("javascript:"):
                return absolute(href, page_url)
    return None


def crawl_category(start_url: str) -> list[str]:
    found: list[str] = []
    seen_pages: set[str] = set()
    page_url: str | None = start_url
    while page_url and page_url not in seen_pages:
        seen_pages.add(page_url)
        html = fetch(page_url)
        for link in article_links(html, page_url):
            if link not in found:
                found.append(link)
        page_url = next_link(html, page_url)
        print(f"  list page {len(seen_pages)}: {len(found)} article URLs")
    return found


def clean_text(value: str) -> str:
    value = re.sub(r"[ \t\r\f\v]+", " ", value)
    return re.sub(r"\n{3,}", "\n\n", value).strip()


def chinese_chars(*values: str) -> list[str]:
    result: list[str] = []
    for value in values:
        for char in value:
            if "\u4e00" <= char <= "\u9fff" and char not in result:
                result.append(char)
                if len(result) == 8:
                    return result
    return result


def parse_article(start_url: str, style: str) -> dict:
    image_urls: list[str] = []
    source_urls: list[str] = []
    descriptions: list[str] = []
    title = ""
    author = "佚名"
    first_url: str | None = start_url
    while first_url and first_url not in source_urls:
        page_url = first_url
        html = fetch(page_url)
        source_urls.append(page_url)
        soup = BeautifulSoup(html, "lxml")
        article = soup.select_one("article")
        if article is None:
            break
        if not title:
            title = clean_text(article.select_one("h1").get_text(" ", strip=True)) if article.select_one("h1") else ""
            author_match = re.search(r"作者\s*[:：]\s*([^\s阅读]+)", article.get_text(" ", strip=True))
            if author_match:
                author = author_match.group(1)
        content = article.select_one("#newsContent")
        if content:
            for image in content.select("img[src]"):
                image_url = absolute(image["src"], page_url)
                image_path = urlparse(image_url).path.lower()
                is_site_asset = "/images/" in image_path or "/inc_img/" in image_path
                is_image = image_path.endswith((".jpg", ".jpeg", ".png", ".webp", ".gif"))
                if "/upfiles/" in image_path and is_image and not is_site_asset and image_url not in image_urls:
                    image_urls.append(image_url)
            for node in content.select("script, style, .navPageBox, .clear"):
                node.decompose()
            text = clean_text(content.get_text("\n", strip=True))
            if text:
                descriptions.append(text)
        first_url = next_link(html, page_url)

    match = re.search(r"/news/(\d+)(?:_\d+)?\.html$", start_url)
    numeric_id = match.group(1) if match else hashlib.sha1(start_url.encode()).hexdigest()[:12]
    description = clean_text("\n\n".join(dict.fromkeys(descriptions)))
    dynasty = infer_dynasty(title + description)
    return {
        "id": f"-yac8-{numeric_id}",
        "tid": "",
        "title": title or numeric_id,
        "author": author,
        "dynasty": dynasty,
        "style": style,
        "description": description,
        "imageUrls": image_urls,
        "characters": chinese_chars(title, description),
        "sourceUrl": source_urls,
    }


def crawl_one(url: str, style: str) -> dict | None:
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            post = parse_article(url, style)
            if post["title"] and post["imageUrls"]:
                return post
        except Exception as error:
            last_error = error
        time.sleep(0.5 * (attempt + 1))
    if last_error:
        print(f"  ERROR {url}: {last_error}", flush=True)
    return None


def infer_dynasty(text: str) -> str:
    patterns = [
        ("秦", "秦"), ("西汉|东汉|汉代|汉", "汉"), ("魏晋|东晋|西晋|晋代|晋", "晋"),
        ("隋", "隋"), ("唐", "唐"), ("北宋|南宋|宋代|宋", "宋"),
        ("元", "元"), ("明", "明"), ("清", "清"), ("民国", "民国"),
        ("现代|当代", "现代"),
    ]
    for pattern, dynasty in patterns:
        if re.search(pattern, text):
            return dynasty
    return "佚名"


def assign_tids(posts: Iterable[dict], used: set[str]) -> None:
    for post in posts:
        if post.get("tid"):
            used.add(post["tid"])
            continue
        digest = hashlib.sha1(post["id"].encode()).hexdigest().upper()
        for offset in range(0, len(digest) - 3, 4):
            candidate = "#" + digest[offset:offset + 4]
            if candidate not in used:
                post["tid"] = candidate
                used.add(candidate)
                break
        else:
            raise RuntimeError(f"unable to allocate tid for {post['id']}")


def write_json(path: Path, posts: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as handle:
        json.dump(posts, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
        temporary = Path(handle.name)
    temporary.replace(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--category", choices=[*CATEGORIES, "all"], default="all")
    parser.add_argument("--delay", type=float, default=0.35)
    parser.add_argument("--workers", type=int, default=6)
    parser.add_argument("--max-posts", type=int, default=0)
    parser.add_argument("--output", type=Path, default=OUTPUT)
    parser.add_argument("--progress", type=Path, default=PROGRESS)
    args = parser.parse_args()

    bootstrap_cookies()

    raw_existing = json.loads(args.output.read_text(encoding="utf-8"))
    existing = list({post["id"]: post for post in raw_existing}.values())
    progress = json.loads(args.progress.read_text(encoding="utf-8")) if args.progress.exists() else {}
    category_names = CATEGORIES if args.category == "all" else {args.category: CATEGORIES[args.category]}

    urls_by_category = progress.get("urls", {})
    all_urls: list[tuple[str, str]] = []
    for key, (label, path, style) in category_names.items():
        print(f"[{label}] collecting list pages")
        urls = urls_by_category.get(key) or crawl_category(absolute(path, BASE_URL))
        urls_by_category[key] = urls
        all_urls.extend((url, style) for url in urls)
        progress["urls"] = urls_by_category
        write_json(args.progress, progress)

    completed = {post["id"]: post for post in progress.get("posts", [])}
    existing_ids = {post["id"] for post in existing}
    pending = []
    for url, style in all_urls:
        numeric = re.search(r"/news/(\d+)", url)
        post_id = f"-yac8-{numeric.group(1)}" if numeric else ""
        if post_id not in completed and post_id not in existing_ids:
            pending.append((url, style))
    if args.max_posts:
        pending = pending[:args.max_posts]

    processed = 0
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as executor:
        futures = [executor.submit(crawl_one, url, style) for url, style in pending]
        for future in as_completed(futures):
            post = future.result()
            if post is None:
                continue
            completed[post["id"]] = post
            processed += 1
            print(f"  [{processed}/{len(pending)}] {post['title']} ({len(post['imageUrls'])} images, {len(post['sourceUrl'])} pages)", flush=True)
            progress["posts"] = list(completed.values())
            write_json(args.progress, progress)
            if args.delay:
                time.sleep(args.delay)

    new_posts = [post for post in completed.values() if post["id"] not in existing_ids]
    assign_tids(existing, {post["tid"] for post in existing if post.get("tid")})
    assign_tids(new_posts, {post["tid"] for post in existing if post.get("tid")})
    merged = existing + new_posts
    write_json(args.output, merged)
    print(f"Done: {len(new_posts)} crawled, {len(merged)} total posts")


if __name__ == "__main__":
    main()
