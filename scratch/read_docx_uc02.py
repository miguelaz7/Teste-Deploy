import zipfile
import xml.etree.ElementTree as ET
import sys
import os

def search_docx(docx_path, term):
    if not os.path.exists(docx_path):
        print(f"File not found: {docx_path}")
        return
        
    try:
        with zipfile.ZipFile(docx_path, 'r') as z:
            if 'word/document.xml' not in z.namelist():
                print("No word/document.xml found")
                return
            with z.open('word/document.xml') as f:
                tree = ET.parse(f)
                root = tree.getroot()
                paras = []
                for p in root.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}p'):
                    texts = []
                    for t in p.iter('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}t'):
                        if t.text:
                            texts.append(t.text)
                    if texts:
                        paras.append("".join(texts))
                        
                print(f"Total paragraphs: {len(paras)}")
                capture = False
                capture_count = 0
                for i, text in enumerate(paras):
                    if term.lower() in text.lower():
                        capture = True
                        capture_count = 0
                        print(f"\n=== Found '{term}' at paragraph {i} ===")
                    if capture:
                        print(f"[{i}]: {text}")
                        capture_count += 1
                        if capture_count > 30: # limit output per match
                            capture = False
                            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    search_docx(sys.argv[1], sys.argv[2])
