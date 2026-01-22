# 📧 Brevo Setup Guide - FREE Email Service (No Account Rejection!)

**Perfect alternative!** Brevo (formerly Sendinblue) is a reliable free email service that's easy to sign up for.

---

## ✅ Why Brevo?

- ✅ **300 emails/day FREE** (9,000/month) - More than SendGrid!
- ✅ **No domain required** - can send to any email address
- ✅ **No credit card required**
- ✅ **Easy signup** - No account rejection issues
- ✅ **HTTP API** (works on Render)
- ✅ **Great deliverability**

---

## 📝 STEP 1: Create Brevo Account

1. Go to **[brevo.com](https://www.brevo.com)** (or [sendinblue.com](https://www.sendinblue.com))
2. Click **"Sign up free"** (top right)
3. Fill in:
   - **Email:** Your email address
   - **Password:** Create a password
   - **First Name:** Your first name
   - **Last Name:** Your last name
   - **Company:** "Personal Project" (optional)
4. Click **"Create my free account"**
5. Verify your email (check inbox for verification link)
6. Complete the onboarding (skip optional steps if you want)

**Note:** Brevo has much easier signup than SendGrid - no account rejection issues!

---

## 🔑 STEP 2: Get API Key

1. In Brevo dashboard, click your **profile icon** (top right)
2. Go to **"SMTP & API"** → **"API Keys"** (or go to [app.brevo.com/settings/keys/api](https://app.brevo.com/settings/keys/api))
3. Click **"Generate a new API key"**
4. Fill in:
   - **Name:** `BillSplit Production` (or any name)
   - **Permissions:** Select **"Send emails"** (or "Manage account" for full access)
5. Click **"Generate"**
6. **IMPORTANT:** Copy the API key immediately!
   - It looks like: `xkeysib-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-xxxxxxxxxxxx`
   - **You can only see it once!** If you lose it, create a new one.
7. **Save this API key** - you'll need it for Render

---

## 📧 STEP 3: Verify Sender Email (Quick & Easy)

Brevo requires you to verify a sender email (very simple):

1. Go to **"SMTP & API"** → **"Senders"** (or [app.brevo.com/settings/senders](https://app.brevo.com/settings/senders))
2. Click **"Add a sender"**
3. Fill in:
   - **Email:** Your email (e.g., `tarushbatra11318@gmail.com`)
   - **Name:** `BillSplit` (or your app name)
   - **Company:** Your name or "Personal Project" (optional)
4. Click **"Save"**
5. **Check your email inbox** for verification email from Brevo
6. **Click the verification link** in the email
7. Status will change to **"Verified"** ✅

**That's it!** Much simpler than SendGrid's process.

---

## 🔧 STEP 4: Configure Render Environment Variables

1. Go to **Render Dashboard** → Backend Service (`billsplit-backend`)
2. Click **"Environment"** tab
3. Add/Update these variables:

### Add BREVO_API_KEY:

- **Key:** `BREVO_API_KEY`
- **Value:** Your API key from Step 2 (starts with `xkeysib-`)
- Click **"Save Changes"**

### Add BREVO_FROM_EMAIL:

- **Key:** `BREVO_FROM_EMAIL`
- **Value:** Your verified email from Step 3 (e.g., `tarushbatra11318@gmail.com`)
- Click **"Save Changes"**

### Add BREVO_FROM_NAME (Optional):

- **Key:** `BREVO_FROM_NAME`
- **Value:** `BillSplit` (or your app name)
- Click **"Save Changes"**

### Remove Old Variables (Optional):

You can remove these (they're no longer used):
- `SENDGRID_API_KEY`
- `SENDGRID_FROM_EMAIL`
- `RESEND_API_KEY`
- `RESEND_FROM_EMAIL`

---

## 🚀 STEP 5: Deploy and Test

1. **Push your code to GitHub:**
   ```bash
   git push origin main
   ```

2. **Wait for Render to redeploy** (2-3 minutes)

3. **Test email configuration:**
   - Open: `https://[your-backend-url].onrender.com/api/health`
   - You should see:
     ```json
     {
       "status": "UP",
       "email_configured": "true",
       "brevo_api_key_set": "YES",
       "brevo_from_email_set": "YES",
       "email_provider": "Brevo API (Free: 300 emails/day)"
     }
     ```

4. **Test sending an email:**
   - Open: `https://[your-backend-url].onrender.com/api/health/test-email?to=sanjeevsonia3@gmail.com`
   - Should return: `{"status": "success", ...}`
   - **Check inbox!** Email should arrive within seconds! 🎉

---

## ✅ STEP 6: Verify It Works

1. **Send a test email** using the test endpoint
2. **Check your inbox** - email should arrive within 10-30 seconds
3. **Try inviting a member** in your BillSplit app
4. **Check if they receive the invitation email**

---

## 🎉 Success Checklist

- [ ] Brevo account created
- [ ] API key generated and copied
- [ ] Sender email verified
- [ ] `BREVO_API_KEY` set in Render
- [ ] `BREVO_FROM_EMAIL` set in Render (your verified email)
- [ ] `BREVO_FROM_NAME` set in Render (optional)
- [ ] Code pushed to GitHub
- [ ] Render redeployed
- [ ] `/api/health` shows `email_configured: true`
- [ ] Test email sent successfully
- [ ] Email received in inbox! ✅
- [ ] Can invite members and they receive emails! ✅

---

## 🚨 Troubleshooting

### Issue: `email_configured: false`

**Solution:**
- Check `BREVO_API_KEY` is set correctly in Render
- Check `BREVO_FROM_EMAIL` is set correctly
- Make sure there are no extra spaces in the values

### Issue: Test email returns error

**Check Render logs:**
1. Go to Render Dashboard → Backend Service → "Logs" tab
2. Look for error messages
3. Common errors:
   - `401 Unauthorized` → API key is wrong
   - `400 Bad Request` → Sender email not verified or invalid format

### Issue: "Sender email not verified"

**Solution:**
1. Go to Brevo → SMTP & API → Senders
2. Check if your sender is verified
3. If not, check your email inbox for verification link
4. Click the verification link
5. Wait for status to change to "Verified"

### Issue: Email not received

**Solutions:**
1. Check spam folder
2. Wait 30-60 seconds (sometimes takes a moment)
3. Check Brevo dashboard → Statistics → Email activity
4. See if email was sent and delivery status

### Issue: Rate limiting

**Solution:**
- Free tier: 300 emails/day (9,000/month)
- If you hit the limit, wait until next day or upgrade

---

## 📊 Brevo Dashboard

You can monitor your emails in Brevo:

1. Go to Brevo dashboard
2. Click **"Statistics"** → **"Email activity"**
3. See all sent emails, delivery status, opens, bounces, etc.
4. Very useful for debugging!

---

## 💰 Cost

- **Free Tier:** 300 emails/day (9,000/month)
- **Paid Plans:** Start at $25/month (if you need more)

**For your BillSplit app, the free tier is perfect!**

---

## 🎯 Why Brevo Over SendGrid?

| Feature | SendGrid | Brevo |
|---------|----------|-------|
| **Free tier** | 100/day | 300/day ✅ |
| **Account signup** | Can be rejected ❌ | Easy ✅ |
| **Domain required?** | No | No |
| **Can send to any email?** | Yes | Yes |
| **Easy verification** | Complex | Simple ✅ |

---

## 🎉 Congratulations!

Your email functionality is now working with Brevo! You can send invitations to ANY email address without needing a custom domain, and no account rejection issues!

**Questions?** Check Brevo docs: [developers.brevo.com](https://developers.brevo.com)

---

**Happy Email Sending! 📧✨**
