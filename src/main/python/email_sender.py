import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import sys
import traceback

class EmailSender:
    def __init__(self):
        self.sender_email = "henialoui964@gmail.com"
        self.sender_password = "ahciowiumzqcemfw"
        self.smtp_server = "smtp.gmail.com"
        self.smtp_port = 587

    def send_application_confirmation(self, recipient_email, first_name, project_name, project_desc, start_date, end_date):
        try:
            print(f"Attempting to send email to {recipient_email}")
            print(f"Using SMTP server: {self.smtp_server}:{self.smtp_port}")

            # Create message
            msg = MIMEMultipart()
            msg['From'] = self.sender_email
            msg['To'] = recipient_email
            msg['Subject'] = "Project Application Confirmation"

            # Email body
            body = f"""
Dear {first_name},

Thank you for applying to the project "{project_name}". We have received your application and will review it shortly.

Application Details:
- Project: {project_name}
- Description: {project_desc}
- Dates: {start_date} to {end_date}
- Status: Under Review

We will contact you within 5 business days regarding the next steps.

Best regards,
Your Project Team

Note: This is an automated message. Please do not reply to this email.
            """

            msg.attach(MIMEText(body, 'plain'))

            print("Connecting to SMTP server...")
            # Create SMTP session
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            print("Starting TLS...")
            server.starttls()
            print("Logging in...")
            server.login(self.sender_email, self.sender_password)
            print("Sending message...")
            server.send_message(msg)
            print("Message sent successfully!")
            server.quit()
            return True

        except Exception as e:
            print("Error sending email:")
            print(f"Error type: {type(e).__name__}")
            print(f"Error message: {str(e)}")
            print("Full traceback:")
            traceback.print_exc()
            return False

    def send_status_update(self, recipient_email, first_name, project_name, new_status):
        try:
            msg = MIMEMultipart()
            msg['From'] = self.sender_email
            msg['To'] = recipient_email
            msg['Subject'] = f"Update on Your Application for {project_name}"

            body = f"""
Dear {first_name},

This is to inform you that the status of your application for the project "{project_name}" has changed.

New Status: {new_status}

If you have any questions, please contact the project team.

Best regards,
Your Project Team

Note: This is an automated message. Please do not reply to this email.
            """

            msg.attach(MIMEText(body, 'plain'))

            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()
            server.login(self.sender_email, self.sender_password)
            server.send_message(msg)
            server.quit()
            print("Status update email sent successfully!")
            return True
        except Exception as e:
            print("Error sending status update email:")
            print(f"Error type: {type(e).__name__}")
            print(f"Error message: {str(e)}")
            traceback.print_exc()
            return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python email_sender.py <mode> ...")
        sys.exit(1)

    mode = sys.argv[1]

    sender = EmailSender()

    if mode == "application_confirmation":
        if len(sys.argv) != 8:
            print("Usage: python email_sender.py application_confirmation <recipient_email> <first_name> <project_name> <project_desc> <start_date> <end_date>")
            sys.exit(1)
        recipient_email = sys.argv[2]
        first_name = sys.argv[3]
        project_name = sys.argv[4]
        project_desc = sys.argv[5]
        start_date = sys.argv[6]
        end_date = sys.argv[7]
        success = sender.send_application_confirmation(recipient_email, first_name, project_name, project_desc, start_date, end_date)
    elif mode == "status_update":
        if len(sys.argv) != 6:
            print("Usage: python email_sender.py status_update <recipient_email> <first_name> <project_name> <new_status>")
            sys.exit(1)
        recipient_email = sys.argv[2]
        first_name = sys.argv[3]
        project_name = sys.argv[4]
        new_status = sys.argv[5]
        success = sender.send_status_update(recipient_email, first_name, project_name, new_status)
    else:
        print("Unknown mode")
        sys.exit(1)

    if not success:
        sys.exit(1)