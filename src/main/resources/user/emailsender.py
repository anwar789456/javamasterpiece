import smtplib
import sys
from email.mime.text import MIMEText

def send_email():
    sender_email = "anouarsalhi123@gmail.com"
    app_password = "emkn kcjd gecb wrkc"
    receiver_email = sys.argv[1]
    subject = sys.argv[2]
    body = sys.argv[3]

    msg = MIMEText(body)
    msg['Subject'] = subject
    msg['From'] = sender_email
    msg['To'] = receiver_email

    try:
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.starttls()
        server.login(sender_email, app_password)
        server.sendmail(sender_email, receiver_email, msg.as_string())
        server.quit()
        print("Email sent successfully!")
    except Exception as e:
        print(f"Error sending email: {e}")
        sys.exit(1)

if __name__ == "__main__":
    send_email()
