import sys
import traceback
import re
import cv2
import pytesseract

def check_packages():
    missing_packages = []
    try:
        import cv2
    except ImportError:
        missing_packages.append("opencv-python")
    try:
        import pytesseract
    except ImportError:
        missing_packages.append("pytesseract")
    
    if missing_packages:
        print(f"Error: Missing required Python packages: {', '.join(missing_packages)}", file=sys.stderr)
        print("Please install them using: pip install " + " ".join(missing_packages), file=sys.stderr)
        return False
    return True

if not check_packages():
    sys.exit(1)

pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

def extract_numbers_from_camera():
    try:
        # Print available cameras
        print("Checking available cameras...", file=sys.stderr)
        for i in range(2):
            cap = cv2.VideoCapture(i)
            if cap.isOpened():
                print(f"Camera {i} is available", file=sys.stderr)
                cap.release()
            else:
                print(f"Camera {i} is not available", file=sys.stderr)

        # Try camera 0 first, then 1 if that fails
        video_capture = cv2.VideoCapture(0)
        if not video_capture.isOpened():
            print("Camera 0 not available, trying camera 1...", file=sys.stderr)
            video_capture = cv2.VideoCapture(1)
        
        if not video_capture.isOpened():
            print("Error: No cameras available", file=sys.stderr)
            return

        print("Camera opened successfully", file=sys.stderr)
        print("Scanning for CIN... Press 'q' to quit", file=sys.stderr)
        
        while True:
            ret, frame = video_capture.read()
            if not ret:
                print("Error: Unable to capture frame", file=sys.stderr)
                break
                
            # Improve image quality for OCR
            gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            # Apply thresholding to get better contrast
            _, threshold = cv2.threshold(gray_frame, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            
            # Extract text
            extracted_text = pytesseract.image_to_string(threshold)
            
            # Look for numbers that could be CINs (7-10 digits)
            numbers = re.findall(r'\b\d{7,10}\b', extracted_text)
            
            # Show the frame
            cv2.imshow("Camera Feed (Press 'q' to quit)", frame)
            
            if numbers:
                print(numbers[0])  # Print first found number
                break
                
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
        
        video_capture.release()
        cv2.destroyAllWindows()
        
    except Exception as e:
        print(f"Error in extract_numbers_from_camera: {str(e)}", file=sys.stderr)
        print(traceback.format_exc(), file=sys.stderr)

if __name__ == "__main__":
    extract_numbers_from_camera()