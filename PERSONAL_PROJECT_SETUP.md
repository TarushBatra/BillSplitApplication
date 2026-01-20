# 🚀 Personal Project Setup - Quick Reference

Perfect for personal projects! No custom domain needed.

---

## ✅ Current Setup (Using Test Domain)

You're using **Resend's test domain** which is perfect for personal projects:

- **From Email:** `onboarding@resend.dev`
- **Free:** 3,000 emails/month
- **No domain purchase needed**
- **Works immediately**

---

## 🔧 Render Environment Variables

Make sure these are set in **Render Dashboard → Backend Service → Environment**:

### Required Variables:

1. **`RESEND_API_KEY`**
   - Get from: [resend.com/api-keys](https://resend.com/api-keys)
   - Value: `re_xxxxx...` (your API key)

2. **`RESEND_FROM_EMAIL`**
   - Value: `onboarding@resend.dev`
   - This is the test domain - perfect for personal projects!

### Optional Variables:

3. **`APP_NAME`** (Optional)
   - Value: `BillSplit` (or your app name)
   - Used in email branding

4. **`APP_URL`** (Required for email links)
   - Value: `https://billsplit-frontend.onrender.com` (your frontend URL)

5. **`CORS_ALLOWED_ORIGINS`** (Required)
   - Value: `https://billsplit-frontend.onrender.com` (your frontend URL)

---

## 📧 Email Features (Already Working!)

Your emails now have:

- ✅ **HTML formatting** - Professional looking emails
- ✅ **Branded design** - Uses your app name
- ✅ **Action buttons** - Clickable links to your app
- ✅ **Mobile responsive** - Looks great on all devices
- ✅ **All email types:**
  - Group invitations
  - Expense notifications
  - Settlement summaries
  - Invitation rejections

---

## 🎯 Quick Checklist

- [ ] `RESEND_API_KEY` set in Render
- [ ] `RESEND_FROM_EMAIL` = `onboarding@resend.dev` in Render
- [ ] `APP_URL` set to your frontend URL
- [ ] `CORS_ALLOWED_ORIGINS` set to your frontend URL
- [ ] Backend deployed on Render
- [ ] Test email sent successfully ✅

---

## 🧪 Testing

### Test Email Configuration:
```
https://[your-backend-url].onrender.com/api/health
```

Should show:
```json
{
  "status": "UP",
  "email_configured": "true",
  "resend_from_email_set": "YES",
  "email_provider": "Resend API"
}
```

### Test Sending Email:
```
https://[your-backend-url].onrender.com/api/health/test-email?to=your-email@gmail.com
```

---

## ⚠️ Important Notes

### Test Domain Limitations:

1. **Spam Folder:** 
   - Emails from `onboarding@resend.dev` may sometimes go to spam
   - Ask recipients to check spam folder
   - Mark as "Not Spam" if needed

2. **Professional Appearance:**
   - Test domain is fine for personal projects
   - If you want more professional look later, you can add a custom domain (see `DOMAIN_SETUP_GUIDE.md`)

3. **Free Tier:**
   - 3,000 emails/month is plenty for personal projects
   - Monitor usage in Resend dashboard

---

## 💡 When to Upgrade to Custom Domain

You might want a custom domain later if:

- ❌ Emails frequently go to spam
- ❌ You want a more professional look
- ❌ You're sharing the app with many users
- ❌ You want your own brand in the "From" field

**But for now, `onboarding@resend.dev` is perfect!** ✅

---

## 🎉 You're All Set!

Your email system is production-ready with:

- ✅ HTML emails
- ✅ Professional design
- ✅ All features working
- ✅ Free tier (3,000 emails/month)
- ✅ No domain purchase needed

**Just make sure `RESEND_FROM_EMAIL=onboarding@resend.dev` is set in Render!**

---

## 📚 Need Help?

- **Resend Dashboard:** [resend.com](https://resend.com)
- **Check Email Logs:** Resend Dashboard → Emails
- **Test Endpoint:** `/api/health/test-email?to=your-email@gmail.com`

**Happy Coding! 🚀**
