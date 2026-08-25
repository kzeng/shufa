# Crawl Zitiewang.com Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Crawl all calligraphy posts from zitiewang.com across 9 categories and merge with existing 91 posts into posts.json

**Architecture:** Python script that crawls category pages to collect post URLs, then fetches detail pages to extract metadata and images. Uses requests + BeautifulSoup for HTML parsing. Saves progress incrementally to JSON files.

**Tech Stack:** Python 3, requests, beautifulsoup4, json, time (for rate limiting)

---

## Task 1: Setup Python Environment

**Files:**
- Create: `scripts/crawl_zitiewang.py`
- Create: `scripts/requirements.txt`

**Step 1: Create requirements.txt**

```
requests>=2.31.0
beautifulsoup4>=4.12.0
lxml>=4.9.0
```

**Step 2: Install dependencies**

Run: `pip install -r scripts/requirements.txt`

**Step 3: Create base script structure**

```python
#!/usr/bin/env python3
"""Crawl zitiewang.com to collect calligraphy posts."""

import json
import time
import re
import hashlib
from pathlib import Path
from typing import Dict, List, Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://www.zitiewang.com"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
OUTPUT_DIR = Path("app/src/main/assets")
PROGRESS_DIR = Path("scripts/progress")

# Category definitions
CATEGORIES = {
    "qinhan": {"name": "秦汉书法", "url": "/shufa/qinhan/"},
    "weijin": {"name": "魏晋书法", "url": "/shufa/weijin/"},
    "suitang": {"name": "隋唐书法", "url": "/shufa/suitang/"},
    "songchao": {"name": "宋朝书法", "url": "/shufa/songchao/"},
    "yuanchao": {"name": "辽金元书法", "url": "/shufa/yuanchao/"},
    "mingchao": {"name": "明朝书法", "url": "/shufa/mingchao/"},
    "qingchao": {"name": "清朝书法", "url": "/shufa/qingchao/"},
    "xiandai": {"name": "近现代书法", "url": "/shufa/xiandai/"},
    "shufa": {"name": "书法欣赏", "url": "/shufa/shufa/"},
}

# Style mapping
STYLE_MAP = {
    "楷书": "KAISHU",
    "小楷": "KAISHU",
    "正书": "KAISHU",
    "行书": "XINGSHU",
    "行楷": "XINGSHU",
    "行草": "XINGSHU",
    "草书": "CAOSHU",
    "章草": "CAOSHU",
    "狂草": "CAOSHU",
    "隶书": "LISHU",
    "篆书": "ZHUANSHU",
    "大篆": "ZHUANSHU",
    "小篆": "ZHUANSHU",
}
```

**Step 4: Commit**

```bash
git add scripts/crawl_zitiewang.py scripts/requirements.txt
git commit -m "feat: add web crawler script skeleton"
```

---

## Task 2: Implement Category Page Crawler

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add category crawling function**

```python
def fetch_page(url: str) -> Optional[str]:
    """Fetch a page and return its HTML content."""
    try:
        full_url = urljoin(BASE_URL, url)
        response = requests.get(full_url, headers=HEADERS, timeout=30)
        response.raise_for_status()
        return response.text
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None


def get_post_urls_from_category(category_url: str) -> List[str]:
    """Extract all post URLs from a category, handling pagination."""
    post_urls = []
    page = 0
    
    while True:
        if page == 0:
            url = category_url
        else:
            url = f"{category_url}index{page}.htm"
        
        print(f"Fetching: {url}")
        html = fetch_page(url)
        if not html:
            break
        
        soup = BeautifulSoup(html, "lxml")
        
        # Find all post links in the category listing
        # Pattern: /shufa/XXXXX.htm
        links = soup.find_all("a", href=re.compile(r"^/shufa/[a-z]+\d+\.htm$"))
        
        if not links:
            break
        
        for link in links:
            href = link.get("href")
            if href and href not in post_urls:
                post_urls.append(href)
        
        # Check if there's a next page
        pagelist = soup.find("div", class_="pagelist")
        if not pagelist:
            break
        
        next_link = pagelist.find("a", string="下一页")
        if not next_link:
            break
        
        page += 1
        time.sleep(1)  # Rate limiting
    
    return post_urls


def crawl_all_categories() -> Dict[str, List[str]]:
    """Crawl all categories and return dict of category -> post URLs."""
    category_posts = {}
    
    for cat_key, cat_info in CATEGORIES.items():
        print(f"\n=== Crawling {cat_info['name']} ===")
        post_urls = get_post_urls_from_category(cat_info["url"])
        category_posts[cat_key] = post_urls
        print(f"Found {len(post_urls)} posts in {cat_info['name']}")
        
        # Save progress
        save_progress("category_urls", category_posts)
        time.sleep(2)
    
    return category_posts
```

**Step 2: Add progress saving function**

```python
def save_progress(name: str, data):
    """Save progress to a JSON file."""
    PROGRESS_DIR.mkdir(exist_ok=True)
    filepath = PROGRESS_DIR / f"{name}.json"
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def load_progress(name: str):
    """Load progress from a JSON file."""
    filepath = PROGRESS_DIR / f"{name}.json"
    if filepath.exists():
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
    return None
```

**Step 3: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add category page crawler"
```

---

## Task 3: Implement Detail Page Parser

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add detail page parsing function**

```python
def parse_detail_page(html: str, url: str) -> Optional[Dict]:
    """Parse a detail page and extract post data."""
    soup = BeautifulSoup(html, "lxml")
    
    # Extract title
    title_tag = soup.find("h1")
    if not title_tag:
        return None
    title = title_tag.get_text(strip=True)
    
    # Extract style from page info
    style = "KAISHU"  # Default
    page_info = soup.find("p")
    if page_info:
        info_text = page_info.get_text()
        for style_cn, style_en in STYLE_MAP.items():
            if style_cn in info_text:
                style = style_en
                break
    
    # Extract description from meta or content
    description = ""
    meta_desc = soup.find("meta", attrs={"name": "description"})
    if meta_desc:
        description = meta_desc.get("content", "")
    
    # If description is too short, try to get from content
    if len(description) < 50:
        content_div = soup.find("div", class_=re.compile(r"^ergim"))
        if content_div:
            # Get first paragraph
            p = content_div.find("p")
            if p:
                description = p.get_text(strip=True)
    
    # Truncate description to 200-400 characters
    if len(description) > 400:
        description = description[:400]
    
    # Extract images
    image_urls = []
    img_tags = soup.find_all("img")
    for img in img_tags:
        src = img.get("data-src") or img.get("src")
        if src and "img.zitiewang.com" in src and not src.endswith("s.jpg"):
            image_urls.append(src)
    
    # Extract author if available
    author = ""
    author_match = re.search(r"作者[：:]\s*(\S+)", soup.get_text())
    if author_match:
        author = author_match.group(1)
    
    # Extract dynasty from title or content
    dynasty = ""
    dynasty_patterns = [
        (r"秦", "秦"), (r"汉|东汉|西汉|新莽", "汉"),
        (r"魏晋|晋|东晋|西晋", "晋"), (r"隋", "隋"),
        (r"唐", "唐"), (r"宋|北宋|南宋", "宋"),
        (r"元", "元"), (r"明", "明"), (r"清", "清"),
        (r"民国", "民国"), (r"现代|当代", "现代"),
    ]
    for pattern, dyn in dynasty_patterns:
        if re.search(pattern, title + description):
            dynasty = dyn
            break
    
    # Generate unique ID from URL
    post_id = url.split("/")[-1].replace(".htm", "")
    
    return {
        "id": post_id,
        "title": title,
        "author": author,
        "dynasty": dynasty,
        "style": style,
        "description": description,
        "imageUrls": image_urls,
        "characters": [],
    }
```

**Step 2: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add detail page parser"
```

---

## Task 4: Implement Character Extraction

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add character extraction from title**

```python
def extract_characters(title: str, description: str) -> List[str]:
    """Extract representative characters from title and description."""
    # Extract unique Chinese characters from title
    chars = []
    seen = set()
    
    for char in title:
        if "\u4e00" <= char <= "\u9fff" and char not in seen:
            chars.append(char)
            seen.add(char)
    
    # Add a few from description if needed
    if len(chars) < 5:
        for char in description:
            if "\u4e00" <= char <= "\u9fff" and char not in seen:
                chars.append(char)
                seen.add(char)
                if len(chars) >= 8:
                    break
    
    return chars[:8]
```

**Step 2: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add character extraction"
```

---

## Task 5: Implement Detail Page Crawler

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add detail page crawling with batching**

```python
def crawl_detail_pages(post_urls: List[str], batch_size: int = 10) -> List[Dict]:
    """Crawl detail pages in batches and return post data."""
    posts = []
    total = len(post_urls)
    
    for i, url in enumerate(post_urls):
        print(f"  [{i+1}/{total}] Fetching: {url}")
        
        html = fetch_page(url)
        if html:
            post_data = parse_detail_page(html, url)
            if post_data:
                # Extract characters
                post_data["characters"] = extract_characters(
                    post_data["title"], post_data["description"]
                )
                posts.append(post_data)
        
        # Rate limiting
        if (i + 1) % batch_size == 0:
            print(f"  Processed {i+1}/{total} posts, sleeping...")
            time.sleep(2)
        else:
            time.sleep(0.5)
        
        # Save progress every 50 posts
        if (i + 1) % 50 == 0:
            save_progress("detail_progress", posts)
    
    return posts
```

**Step 2: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add detail page crawler"
```

---

## Task 6: Implement Merge and Deduplication

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add merge function**

```python
def merge_posts(new_posts: List[Dict], existing_file: str) -> List[Dict]:
    """Merge new posts with existing posts, removing duplicates."""
    # Load existing posts
    existing_posts = []
    existing_path = Path(existing_file)
    if existing_path.exists():
        with open(existing_path, "r", encoding="utf-8") as f:
            existing_posts = json.load(f)
    
    # Create ID set from existing posts
    existing_ids = {p["id"] for p in existing_posts}
    
    # Add new posts that don't exist
    added = 0
    for post in new_posts:
        if post["id"] not in existing_ids:
            existing_posts.append(post)
            existing_ids.add(post["id"])
            added += 1
    
    print(f"Added {added} new posts, total: {len(existing_posts)}")
    return existing_posts
```

**Step 2: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add merge and deduplication"
```

---

## Task 7: Implement Main Script

**Files:**
- Modify: `scripts/crawl_zitiewang.py`

**Step 1: Add main function**

```python
def main():
    """Main crawl function."""
    print("=" * 60)
    print("Zitiewang.com Calligraphy Crawler")
    print("=" * 60)
    
    # Step 1: Crawl all category pages
    print("\n[Phase 1] Crawling category pages...")
    category_posts = crawl_all_categories()
    
    # Collect all unique post URLs
    all_urls = []
    for urls in category_posts.values():
        all_urls.extend(urls)
    all_urls = list(dict.fromkeys(all_urls))  # Remove duplicates while preserving order
    
    print(f"\nTotal unique post URLs: {len(all_urls)}")
    
    # Step 2: Crawl detail pages
    print("\n[Phase 2] Crawling detail pages...")
    new_posts = crawl_detail_pages(all_urls)
    
    print(f"\nSuccessfully parsed {len(new_posts)} posts")
    
    # Step 3: Merge with existing posts
    print("\n[Phase 3] Merging with existing posts...")
    output_file = OUTPUT_DIR / "posts.json"
    final_posts = merge_posts(new_posts, str(output_file))
    
    # Step 4: Save final result
    print("\n[Phase 4] Saving final posts.json...")
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(final_posts, f, ensure_ascii=False, indent=4)
    
    print(f"\nDone! Total posts: {len(final_posts)}")
    print(f"Saved to: {output_file}")


if __name__ == "__main__":
    main()
```

**Step 2: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: complete crawler with main function"
```

---

## Task 8: Test Crawler on Single Category

**Files:**
- Modify: `scripts/crawl_zitiewang.py` (add test mode)

**Step 1: Add test mode flag**

```python
def main():
    """Main crawl function."""
    import sys
    
    test_mode = "--test" in sys.argv
    
    print("=" * 60)
    print("Zitiewang.com Calligraphy Crawler")
    print("=" * 60)
    
    if test_mode:
        print("\n[TEST MODE] Crawling only qinhan category...")
        CATEGORIES_TO_CRAWL = {"qinhan": CATEGORIES["qinhan"]}
    else:
        CATEGORIES_TO_CRAWL = CATEGORIES
    
    # ... rest of main function
```

**Step 2: Run test**

Run: `python scripts/crawl_zitiewang.py --test`

Expected: Should crawl qinhan category and save progress

**Step 3: Commit**

```bash
git add scripts/crawl_zitiewang.py
git commit -m "feat: add test mode for crawler"
```

---

## Task 9: Run Full Crawl

**Files:**
- None (execution only)

**Step 1: Run full crawl**

Run: `python scripts/crawl_zitiewang.py`

Expected: 
- Crawls all 9 categories
- Collects ~1000+ post URLs
- Fetches detail pages
- Merges with existing 91 posts
- Saves to posts.json

**Step 2: Verify results**

Run: `cat app/src/main/assets/posts.json | jq length`

Expected: Should show total post count (91 existing + new posts)

---

## Task 10: Validate Output

**Files:**
- None (validation only)

**Step 1: Check post structure**

Run: `cat app/src/main/assets/posts.json | jq '.[0] | {id, title, author, dynasty, style, description: (.description[:50] + "..."), imageCount: (.imageUrls | length)}'`

Expected: All fields present and valid

**Step 2: Check for duplicates**

Run: `cat app/src/main/assets/posts.json | jq '[.[].id] | unique | length'`

Expected: Same as total length (no duplicates)

**Step 3: Check style distribution**

Run: `cat app/src/main/assets/posts.json | jq '[.[].style] | group_by(.) | map({style: .[0], count: length})'`

Expected: Mix of KAISHU, XINGSHU, CAOSHU, LISHU, ZHUANSHU

---

## Notes

- Rate limiting: 1 second between category pages, 0.5 seconds between detail pages
- Progress saved every 50 posts to avoid losing work
- Existing 91 posts preserved and merged with new posts
- All images from img.zitiewang.com domain
- Descriptions truncated to 200-400 characters
- Characters extracted from title (5-8 per post)
