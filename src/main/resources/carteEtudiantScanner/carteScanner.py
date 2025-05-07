import cv2
import pytesseract
import re
import sys
import os
import time

if os.name == 'nt':
    sys.stdout.reconfigure(encoding='utf-8')

pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

def extract_name_from_camera():
    video_capture = cv2.VideoCapture(0)

    font = cv2.FONT_HERSHEY_SIMPLEX
    scan_interval = 5.0  # Time between scans in seconds
    last_scan_time = time.time() - scan_interval
    scanning_text = "Scanning..."

    while True:
        ret, frame = video_capture.read()
        if not ret:
            print("Error: Unable to capture frame")
            break

        current_time = time.time()
        time_elapsed = current_time - last_scan_time

        display_frame = frame.copy()

        cv2.putText(display_frame,
                    f"Automatic scanning in progress... ({int(scan_interval - time_elapsed if time_elapsed < scan_interval else 0)}s)",
                    (10, 30), font, 0.7, (0, 255, 0), 2)

        # Show the frame
        cv2.imshow("Auto Document Scanner (Press 'q' to quit)", display_frame)

        # Check if it's time to scan
        if time_elapsed >= scan_interval:
            cv2.putText(display_frame, scanning_text, (10, 70), font, 0.7, (0, 0, 255), 2)
            cv2.imshow("Auto Document Scanner (Press 'q' to quit)", display_frame)

            # Process the frame
            gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

            # Apply image enhancement for better OCR
            # Increase contrast
            enhanced = cv2.convertScaleAbs(gray_frame, alpha=1.2, beta=10)
            # Apply adaptive thresholding
            thresh = cv2.adaptiveThreshold(enhanced, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                           cv2.THRESH_BINARY, 11, 2)

            # Try OCR on both the enhanced and original images
            extracted_text_original = pytesseract.image_to_string(gray_frame, lang='fra')
            extracted_text_enhanced = pytesseract.image_to_string(thresh, lang='fra')

            # Combine both results for better chances of detection
            extracted_text = extracted_text_original + "\n" + extracted_text_enhanced

            # Debug: Print extracted text to help diagnose issues

            # print(extracted_text)


            # Try multiple regex patterns to improve matching chances
            nom_match = None
            prenom_match = None

            # Try different patterns for NOM - now handling case for fully uppercase names
            nom_patterns = [
                r'NOM\s*[:\-]?\s*([A-Z]+\s*[A-Z]*)',  # For all uppercase names
                r'NOM\s*[:\-]?\s*(\w+\s*\w*)',
                r'NOM\s*[:\-]?\s*(\S+\s*\S*)',
                r'NOM\s*[:\-]?\s*([^\n\r]+)'          # Match anything up to a newline
            ]

            # Try different patterns for PRÉNOM - now handling multi-part first names
            prenom_patterns = [
                r'PRÉNOM\s*[:\-]?\s*([^\n\r]+)',      # Match anything up to a newline
                r'PRENOM\s*[:\-]?\s*([^\n\r]+)',
                r'PRÉNOM\s*[:\-]?\s*(\w+\s*\w*\s*\w*)',
                r'PRENOM\s*[:\-]?\s*(\w+\s*\w*\s*\w*)'
            ]

            for pattern in nom_patterns:
                nom_match = re.search(pattern, extracted_text, re.IGNORECASE)
                if nom_match:
                    break

            for pattern in prenom_patterns:
                prenom_match = re.search(pattern, extracted_text, re.IGNORECASE)
                if prenom_match:
                    break

            nom = nom_match.group(1).strip() if nom_match else None
            prenom = prenom_match.group(1).strip() if prenom_match else None

            # Clean up the extracted names
            if nom:
                # Remove any lowercase 'l' that might be misrecognized
                nom = re.sub(r'\bl\b', '', nom).strip()
                # Ensure name is in uppercase
                nom = nom.upper()

            if prenom:
                # Fix common OCR errors
                prenom = prenom.replace('|', 'I').replace('l', 'l')
                # Format name properly (first letter uppercase, rest lowercase)
                prenom_parts = prenom.split()
                prenom = ' '.join(p.capitalize() for p in prenom_parts)

            # Show what was found
            # print(f"Found - PRENOM: {prenom}, NOM: {nom}")

            # If both values are found, print the result and exit
            if nom and prenom:
                print(f"{prenom} {nom}")
                break

            # Reset the timer for the next scan
            last_scan_time = current_time

        # Check for 'q' key press to quit
        if cv2.waitKey(1) & 0xFF == ord('q'):
            print("Scanning stopped by user")
            break

    # Clean up
    video_capture.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    extract_name_from_camera()