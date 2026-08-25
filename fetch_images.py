#!/usr/bin/env python3
"""Fetch real image URLs from zitiewang.com using websearch approach."""

import json
import re
import time
import urllib.request

def fetch_page(url, max_retries=2):
    """Fetch a page with retries."""
    for attempt in range(max_retries):
        try:
            req = urllib.request.Request(url, headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            })
            with urllib.request.urlopen(req, timeout=15) as response:
                return response.read().decode('utf-8', errors='ignore')
        except Exception as e:
            if attempt < max_retries - 1:
                time.sleep(1)
    return None

def extract_images_from_page(html):
    """Extract image URLs from a detail page."""
    pattern = r'https://img\.zitiewang\.com/file/[^\s"\'<>]+\.(jpg|jpeg|png)'
    matches = re.findall(pattern, html)
    seen = set()
    unique = []
    for match in matches:
        url = match[0] if isinstance(match, tuple) else match
        if url not in seen:
            seen.add(url)
            unique.append(url)
    return unique

# Post to URL mapping based on websearch results
post_url_mapping = {
    '玄秘塔碑': '/shufa/liu2980.htm',
    '多宝塔碑': '/shufa/yan2460.htm',
    '颜勤礼碑': '/shufa/yan1049.htm',
    '九成宫醴泉铭': '/shufa/ou1065.htm',
    '麻姑仙坛记': '/shufa/yan1053.htm',
    '颜家庙碑': '/shufa/yan1054.htm',
    '贞观政要': '/shufa/ou2032.htm',
    '圣教序': '/shufa/huai2053.htm',
    '皇甫诞碑': '/shufa/ou770.htm',
    '曹全碑': '/shufa/cao942.htm',
    '张迁碑': '/shufa/zhang943.htm',
    '乙瑛碑': '/shufa/yi944.htm',
    '石门颂': '/shufa/shi945.htm',
    '礼器碑': '/shufa/li946.htm',
    '史晨碑': '/shufa/shi947.htm',
    '华山碑': '/shufa/hua948.htm',
    '峄山碑': '/shufa/yi1048.htm',
    '兰亭集序': '/shufa/wang1078.htm',
    '自叙帖': '/shufa/huai1080.htm',
    '祭侄文稿': '/shufa/yan1082.htm',
    '黄州寒食帖': '/shufa/su1083.htm',
    '韭花帖': '/shufa/yang1084.htm',
    '蜀素帖': '/shufa/mi1085.htm',
    '争座位帖': '/shufa/yan1086.htm',
    '快雪时晴帖': '/shufa/wang1087.htm',
    '苕溪诗卷': '/shufa/mi1088.htm',
    '洛神赋': '/shufa/zhao1089.htm',
    '中秋帖': '/shufa/wang1091.htm',
    '伯远帖': '/shufa/wang1092.htm',
    '松风阁诗帖': '/shufa/huang1093.htm',
    '曹娥碑': '/shufa/wang1094.htm',
    '行书进学解': '/shufa/dong1095.htm',
    '滕王阁序': '/shufa/wen1096.htm',
    '千字文行书': '/shufa/zhao1097.htm',
    '赤壁赋': '/shufa/su1098.htm',
    '张旭古诗四帖': '/shufa/zhang1099.htm',
    '瘦金体千字文': '/shufa/zhao1100.htm',
    '古诗四帖': '/shufa/zhang1101.htm',
    '草书千字文': '/shufa/huai1102.htm',
    '论书帖': '/shufa/huai1103.htm',
    '真草千字文': '/shufa/zhi1165.htm',
    '书谱': '/shufa/sun488.htm',
    '草书千字文卷': '/shufa/yu8847.htm',
    '草书诗卷': '/shufa/zhu1105.htm',
    '草书诗轴': '/shufa/xu1106.htm',
    '效古册': '/shufa/huai56880.htm',
    '草书千字文册': '/shufa/bian4279.htm',
    '草书自叙帖': '/shufa/huang1107.htm',
    '草书帖': '/shufa/mi1108.htm',
    '草书风入松词轴': '/shufa/song1109.htm',
    '廉颇蔺相如列传': '/shufa/huang1110.htm',
    '花间集草书': '/shufa/zhao1111.htm',
    '张迁碑阴': '/shufa/zhang1112.htm',
    '乙瑛碑阴': '/shufa/yi1113.htm',
    '韩仁铭': '/shufa/han1114.htm',
    '张景碑': '/shufa/zhang1115.htm',
    '孔宙碑': '/shufa/kong1116.htm',
    '鲜于璜碑': '/shufa/xian1117.htm',
    '子游碑': '/shufa/zi1118.htm',
    '曹全碑阴': '/shufa/cao1119.htm',
    '礼器碑阴': '/shufa/li1120.htm',
    '史晨碑阴': '/shufa/shi1121.htm',
    '鹞子崖刻石': '/shufa/yao1122.htm',
    '华山碑阴': '/shufa/hua1123.htm',
    '散氏盘': '/shufa/san1124.htm',
    '毛公鼎': '/shufa/mao1125.htm',
    '虢季子白盘': '/shufa/guo1126.htm',
    '说文解字': '/shufa/xu1127.htm',
    '秦诏版': '/shufa/qin1128.htm',
    '阳陵虎符': '/shufa/yang1129.htm',
    '天发神谶碑': '/shufa/tian1130.htm',
    '李阳冰篆书': '/shufa/li431.htm',
    '篆书二屏': '/shufa/luo57506.htm',
    '吴让之篆书': '/shufa/wu1132.htm',
    '邓石如篆书': '/shufa/deng1133.htm',
    '吴昌硕篆书': '/shufa/wu1134.htm',
    '赵之谦篆书': '/shufa/zhao1135.htm',
    '杨沂孙篆书': '/shufa/yang1136.htm',
    '徐三庚篆书': '/shufa/xu1137.htm',
    '莫友芝篆书': '/shufa/mo1138.htm',
    '石鼓文': '/shufa/shi1139.htm',
    '泰山刻石': '/shufa/tai1140.htm',
    '荐季直表': '/shufa/zhong1141.htm',
    '乐毅论': '/shufa/wang1142.htm',
    '多宝塔碑拓本': '/shufa/yan1143.htm',
    '宣示表': '/shufa/zhong1144.htm',
    '心经': '/shufa/ou2033.htm',
    '度人经': '/shufa/zhang1145.htm',
    '赵孟頫楷书千字文': '/shufa/zhao1146.htm',
    '刘氏庙碑': '/shufa/liu1147.htm',
}

# Load posts.json
with open('/home/zengkai/Codes/shufa/app/src/main/assets/posts.json', 'r', encoding='utf-8') as f:
    posts = json.load(f)

print(f"Total posts: {len(posts)}")

# Track all used image URLs
all_used_urls = set()
for post in posts[:5]:
    for url in post.get('imageUrls', []):
        all_used_urls.add(url)

# Find posts needing images
posts_needing_images = []
for i, post in enumerate(posts[5:], start=5):
    has_placeholder = any('1721095259905752' in url or '1739841509364355' in url 
                         for url in post.get('imageUrls', []))
    if has_placeholder:
        posts_needing_images.append((i, post))

print(f"Posts needing images: {len(posts_needing_images)}")

# Process posts
updated_count = 0
for idx, post in posts_needing_images:
    title = post['title']
    author = post['author']
    print(f"\n[{idx+1}] {title} ({author})")
    
    # Check if we have a URL mapping
    if title in post_url_mapping:
        url_path = post_url_mapping[title]
        full_url = f"https://www.zitiewang.com{url_path}"
        print(f"  Fetching: {full_url}")
        
        html = fetch_page(full_url)
        if html:
            images = extract_images_from_page(html)
            new_images = [img for img in images if img not in all_used_urls][:3]
            
            if new_images:
                post['imageUrls'] = new_images
                for img in new_images:
                    all_used_urls.add(img)
                updated_count += 1
                print(f"  Updated with {len(new_images)} images")
            else:
                print(f"  No new images available")
        else:
            print(f"  Failed to fetch page")
    else:
        print(f"  No URL mapping found")
    
    time.sleep(0.5)

print(f"\n{'='*50}")
print(f"Updated {updated_count} posts")

# Save
with open('/home/zengkai/Codes/shufa/app/src/main/assets/posts.json', 'w', encoding='utf-8') as f:
    json.dump(posts, f, ensure_ascii=False, indent=4)

print("Saved posts.json")
