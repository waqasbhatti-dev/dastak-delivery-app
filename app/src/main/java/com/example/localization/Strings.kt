package com.example.localization

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    URDU("ur", "Urdu", "اردو")
}

object Strings {
    const val DASTAK_PHONE = "03049457463"

    private val translations = mapOf(
        "app_name" to mapOf(
            AppLanguage.ENGLISH to "Dastak (دستک)",
            AppLanguage.URDU to "دستک (Dastak)"
        ),
        "locality_name" to mapOf(
            AppLanguage.ENGLISH to "Jhelum City Housing (جہلم سٹی ہاؤسنگ)",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ (Jhelum City Housing)"
        ),
        "tagline" to mapOf(
            AppLanguage.ENGLISH to "Fast, reliable on-demand delivery for your home",
            AppLanguage.URDU to "آپ کے گھر کی دہلیز پر فوری اور قابلِ اعتماد ترسیل"
        ),
        "welcome_title" to mapOf(
            AppLanguage.ENGLISH to "Welcome to Dastak",
            AppLanguage.URDU to "دستک میں خوش آمدید"
        ),
        "welcome_desc" to mapOf(
            AppLanguage.ENGLISH to "Your neighborhood delivery service operating exclusively in Jhelum City Housing. Choose your portal below to get started.",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ کے رہائشیوں کے لیے خصوصی آن ڈیمانڈ ڈیلیوری سروس۔ شروع کرنے کے لیے نیچے اپنا پورٹل منتخب کریں۔"
        ),
        "portal_customer" to mapOf(
            AppLanguage.ENGLISH to "Customer Portal",
            AppLanguage.URDU to "کسٹمر پورٹل"
        ),
        "portal_customer_desc" to mapOf(
            AppLanguage.ENGLISH to "Request deliveries, groceries, medicines & track riders",
            AppLanguage.URDU to "گھریلو سودا سلف، ادویات اور ضروری سامان منگوائیں"
        ),
        "portal_rider" to mapOf(
            AppLanguage.ENGLISH to "Rider Portal",
            AppLanguage.URDU to "رائیڈر پورٹل"
        ),
        "portal_rider_desc" to mapOf(
            AppLanguage.ENGLISH to "Apply to become a rider or access active delivery dashboard",
            AppLanguage.URDU to "رائیڈر کے لیے درخواست دیں یا ڈیلیوری ڈیش بورڈ کھولیں"
        ),
        "portal_admin" to mapOf(
            AppLanguage.ENGLISH to "Administrator Portal",
            AppLanguage.URDU to "ایڈمنسٹریٹر پورٹل"
        ),
        "portal_admin_desc" to mapOf(
            AppLanguage.ENGLISH to "Review rider applications, manage orders & app settings",
            AppLanguage.URDU to "رائیڈر کی درخواستوں کی منظوری اور انتظامی کنٹرول"
        ),
        "sign_in_google" to mapOf(
            AppLanguage.ENGLISH to "Sign in with Google",
            AppLanguage.URDU to "گوگل کے ساتھ سائن ان کریں"
        ),
        "sign_in_title" to mapOf(
            AppLanguage.ENGLISH to "Authentication Required",
            AppLanguage.URDU to "لاگ ان درکار ہے"
        ),
        "signing_in" to mapOf(
            AppLanguage.ENGLISH to "Signing in...",
            AppLanguage.URDU to "لاگ ان ہو رہا ہے..."
        ),
        "language_selector" to mapOf(
            AppLanguage.ENGLISH to "Language / زبان",
            AppLanguage.URDU to "زبان / Language"
        ),
        "call_dastak" to mapOf(
            AppLanguage.ENGLISH to "Call Dastak (03049457463)",
            AppLanguage.URDU to "دستک پر کال کریں (03049457463)"
        ),
        "terms_title" to mapOf(
            AppLanguage.ENGLISH to "Mandatory Terms and Conditions",
            AppLanguage.URDU to "لازمی شرائط و ضوابط"
        ),
        "terms_subtitle" to mapOf(
            AppLanguage.ENGLISH to "Please review and accept our operating policies before proceeding to your dashboard.",
            AppLanguage.URDU to "ڈیش بورڈ میں داخل ہونے سے پہلے براہِ کرم ہماری سروس کی شرائط کا جائزہ لیں اور قبول کریں۔"
        ),
        "terms_listen_label" to mapOf(
            AppLanguage.ENGLISH to "Audio Playback (Read Aloud)",
            AppLanguage.URDU to "آڈیو سنیں (بلند آواز میں پڑھیں)"
        ),
        "btn_play" to mapOf(
            AppLanguage.ENGLISH to "Play Audio",
            AppLanguage.URDU to "آڈیو سنیں"
        ),
        "btn_pause" to mapOf(
            AppLanguage.ENGLISH to "Pause",
            AppLanguage.URDU to "وقفہ کریں"
        ),
        "btn_stop" to mapOf(
            AppLanguage.ENGLISH to "Stop",
            AppLanguage.URDU to "بند کریں"
        ),
        "tts_speaking" to mapOf(
            AppLanguage.ENGLISH to "Reading terms aloud...",
            AppLanguage.URDU to "شرائط پڑھی جا رہی ہیں..."
        ),
        "tts_not_supported" to mapOf(
            AppLanguage.ENGLISH to "Audio reading for Urdu is not supported on this device. Please read the written terms below.",
            AppLanguage.URDU to "اس ڈیوائس پر اردو آواز کی سہولت دستیاب نہیں ہے۔ براہِ کرم نیچے دی گئی تحریری شرائط کا مطالعہ فرمائیں۔"
        ),
        "terms_checkbox" to mapOf(
            AppLanguage.ENGLISH to "I have read or listened to the terms and conditions and I agree to follow them.",
            AppLanguage.URDU to "میں نے تمام شرائط و ضوابط پڑھ یا سن لیے ہیں اور میں ان کی پابندی پر رضامند ہوں۔"
        ),
        "btn_accept_continue" to mapOf(
            AppLanguage.ENGLISH to "Accept and Continue",
            AppLanguage.URDU to "قبول کریں اور آگے بڑھیں"
        ),
        "btn_decline" to mapOf(
            AppLanguage.ENGLISH to "Decline and Return",
            AppLanguage.URDU to "انکار کریں اور واپس جائیں"
        ),
        "nav_home" to mapOf(
            AppLanguage.ENGLISH to "Home",
            AppLanguage.URDU to "ہوم"
        ),
        "nav_my_orders" to mapOf(
            AppLanguage.ENGLISH to "My Orders",
            AppLanguage.URDU to "میرے آرڈرز"
        ),
        "nav_history" to mapOf(
            AppLanguage.ENGLISH to "Order History",
            AppLanguage.URDU to "آرڈر کی تاریخ"
        ),
        "nav_complaints" to mapOf(
            AppLanguage.ENGLISH to "Complaints",
            AppLanguage.URDU to "شکایات"
        ),
        "nav_profile" to mapOf(
            AppLanguage.ENGLISH to "Profile",
            AppLanguage.URDU to "پروفائل"
        ),
        "nav_settings" to mapOf(
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.URDU to "سیٹنگز"
        ),
        "nav_logout" to mapOf(
            AppLanguage.ENGLISH to "Logout",
            AppLanguage.URDU to "لاگ آؤٹ"
        ),
        "create_order_title" to mapOf(
            AppLanguage.ENGLISH to "Place an Order",
            AppLanguage.URDU to "نیا آرڈر درج کریں"
        ),
        "item_desc_label" to mapOf(
            AppLanguage.ENGLISH to "What do you need? (e.g. 2 Milk packs, Bread, 1 pack Panadol, 1kg Tomatoes)",
            AppLanguage.URDU to "آپ کو کیا سامان درکار ہے؟ (مثلاً 2 ڈبے دودھ، روٹی، پیناڈول، 1 کلو ٹماٹر)"
        ),
        "voice_request_title" to mapOf(
            AppLanguage.ENGLISH to "Voice Request (Optional)",
            AppLanguage.URDU to "آواز میں فرمائش ریکارڈ کریں (اختیاری)"
        ),
        "record_voice_btn" to mapOf(
            AppLanguage.ENGLISH to "Record Voice Note",
            AppLanguage.URDU to "آواز ریکارڈ کریں"
        ),
        "recording_active" to mapOf(
            AppLanguage.ENGLISH to "Recording... Tap to Stop",
            AppLanguage.URDU to "ریکارڈنگ جاری ہے... روکنے کے لیے دبائیں"
        ),
        "play_voice_btn" to mapOf(
            AppLanguage.ENGLISH to "Play Voice Note",
            AppLanguage.URDU to "ریکارڈنگ سنیں"
        ),
        "delete_voice_btn" to mapOf(
            AppLanguage.ENGLISH to "Remove Voice Note",
            AppLanguage.URDU to "ریکارڈنگ مٹائیں"
        ),
        "upload_photos_label" to mapOf(
            AppLanguage.ENGLISH to "Upload Item Photos (Optional)",
            AppLanguage.URDU to "سامان کی تصویر شامل کریں (اختیاری)"
        ),
        "add_photo_btn" to mapOf(
            AppLanguage.ENGLISH to "Select Photos from Gallery",
            AppLanguage.URDU to "گیلری سے تصویر منتخب کریں"
        ),
        "delivery_address" to mapOf(
            AppLanguage.ENGLISH to "Delivery Address in Jhelum City Housing",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ میں ترسیل کا پتہ"
        ),
        "block_label" to mapOf(
            AppLanguage.ENGLISH to "Sector / Block",
            AppLanguage.URDU to "بلاک / سیکٹر"
        ),
        "street_label" to mapOf(
            AppLanguage.ENGLISH to "Street Number",
            AppLanguage.URDU to "گلی نمبر"
        ),
        "house_label" to mapOf(
            AppLanguage.ENGLISH to "House Number",
            AppLanguage.URDU to "مکان نمبر"
        ),
        "address_instructions_label" to mapOf(
            AppLanguage.ENGLISH to "Additional Address Directions (e.g. Near Mosque, Green Gate)",
            AppLanguage.URDU to "پتہ کی اضافی نشانی (مثلاً مسجد کے قریب، سبز گیٹ)"
        ),
        "transparent_pricing_title" to mapOf(
            AppLanguage.ENGLISH to "Transparent Pricing Breakdown",
            AppLanguage.URDU to "فیس اور چارجز کا شفاف تخمینہ"
        ),
        "platform_fee_info" to mapOf(
            AppLanguage.ENGLISH to "Platform Fee: PKR 4.00 per order",
            AppLanguage.URDU to "پلیٹ فارم فیس: 4.00 روپے فی آرڈر"
        ),
        "service_charge_info" to mapOf(
            AppLanguage.ENGLISH to "Service Charge: 5% of actual merchant purchase bill",
            AppLanguage.URDU to "سروس چارجز: اصل دکاندار کے بل کا 5 فیصد"
        ),
        "delivery_rate_info" to mapOf(
            AppLanguage.ENGLISH to "Delivery Charge: PKR 70.00 per kilometer",
            AppLanguage.URDU to "ڈیلیوری فیس: 70.00 روپے فی کلومیٹر"
        ),
        "receipt_notice" to mapOf(
            AppLanguage.ENGLISH to "Rider will purchase items and submit merchant receipt for your approval before delivery.",
            AppLanguage.URDU to "رائیڈر خریداری کے بعد رسید اپلوڈ کرے گا جسے آپ منظور کریں گے۔"
        ),
        "btn_submit_order" to mapOf(
            AppLanguage.ENGLISH to "Submit Order",
            AppLanguage.URDU to "آرڈر بھیجیں"
        ),
        "order_submitted_success" to mapOf(
            AppLanguage.ENGLISH to "Order submitted successfully!",
            AppLanguage.URDU to "آرڈر کامیابی کے ساتھ جمع ہو گیا!"
        ),
        "available_orders_title" to mapOf(
            AppLanguage.ENGLISH to "Available Orders Nearby",
            AppLanguage.URDU to "دستیاب نئے آرڈرز"
        ),
        "accept_order_btn" to mapOf(
            AppLanguage.ENGLISH to "Accept Order",
            AppLanguage.URDU to "آرڈر قبول کریں"
        ),
        "order_claimed_by_you" to mapOf(
            AppLanguage.ENGLISH to "Order accepted! You are now assigned.",
            AppLanguage.URDU to "آرڈر منظور! یہ آرڈر اب آپ کے سپرد ہے۔"
        ),
        "active_assigned_orders" to mapOf(
            AppLanguage.ENGLISH to "My Active Orders",
            AppLanguage.URDU to "میرے فعال آرڈرز"
        ),
        "order_stage_shopping" to mapOf(
            AppLanguage.ENGLISH to "Start Purchasing (Shopping)",
            AppLanguage.URDU to "خریداری شروع کریں"
        ),
        "submit_bill_btn" to mapOf(
            AppLanguage.ENGLISH to "Submit Purchase Bill & Receipt",
            AppLanguage.URDU to "خریداری کا بل اور رسید بھیجیں"
        ),
        "bill_amount_label" to mapOf(
            AppLanguage.ENGLISH to "Actual Purchase Bill Amount (PKR)",
            AppLanguage.URDU to "خریداری کی اصل رقم (روپے)"
        ),
        "receipt_photo_label" to mapOf(
            AppLanguage.ENGLISH to "Shop Receipt Photo",
            AppLanguage.URDU to "دکاندار کی رسید کی تصویر"
        ),
        "purchased_items_desc" to mapOf(
            AppLanguage.ENGLISH to "Description of Items Purchased",
            AppLanguage.URDU to "خریدے گئے سامان کی تفصیل"
        ),
        "substitutions_label" to mapOf(
            AppLanguage.ENGLISH to "Substitutions / Unavailable Items",
            AppLanguage.URDU to "متبادل سامان یا جو دستیاب نہ ہو سکا"
        ),
        "rider_note_label" to mapOf(
            AppLanguage.ENGLISH to "Note for Customer",
            AppLanguage.URDU to "صارف کے لیے خصوصی پیغام"
        ),
        "distance_km_label" to mapOf(
            AppLanguage.ENGLISH to "Delivery Distance (km)",
            AppLanguage.URDU to "ڈیلیوری فاصلہ (کلومیٹر)"
        ),
        "btn_send_bill" to mapOf(
            AppLanguage.ENGLISH to "Send Bill for Customer Review",
            AppLanguage.URDU to "بل صارف کو منظوری کے لیے بھیجیں"
        ),
        "awaiting_bill_approval" to mapOf(
            AppLanguage.ENGLISH to "Awaiting Customer Bill Approval",
            AppLanguage.URDU to "صارف کی بل منظوری کا انتظار"
        ),
        "bill_review_title" to mapOf(
            AppLanguage.ENGLISH to "Review Rider's Purchase Bill",
            AppLanguage.URDU to "رائیڈر کے خریداری بل کا جائزہ"
        ),
        "accept_bill_btn" to mapOf(
            AppLanguage.ENGLISH to "Accept Bill & Continue",
            AppLanguage.URDU to "بل منظور کریں اور آگے بڑھیں"
        ),
        "reject_bill_btn" to mapOf(
            AppLanguage.ENGLISH to "Reject Bill",
            AppLanguage.URDU to "بل مسترد کریں"
        ),
        "rejection_reason_label" to mapOf(
            AppLanguage.ENGLISH to "Reason for Rejection",
            AppLanguage.URDU to "بل مسترد کرنے کی وجہ"
        ),
        "bill_accepted_banner" to mapOf(
            AppLanguage.ENGLISH to "Customer Accepted Your Bill!",
            AppLanguage.URDU to "صارف نے آپ کا بل منظور کر لیا ہے!"
        ),
        "start_delivery_btn" to mapOf(
            AppLanguage.ENGLISH to "Start Delivery (Out for Delivery)",
            AppLanguage.URDU to "ڈیلیوری کے لیے روانہ ہوں"
        ),
        "complete_delivery_btn" to mapOf(
            AppLanguage.ENGLISH to "Mark as Delivered (Complete)",
            AppLanguage.URDU to "ڈیلیوری مکمل ہو گئی"
        ),
        "order_delivered_banner" to mapOf(
            AppLanguage.ENGLISH to "Order Delivered Successfully",
            AppLanguage.URDU to "آرڈر کامیابی سے پہنچا دیا گیا ہے"
        ),
        "full_name" to mapOf(
            AppLanguage.ENGLISH to "Full Name",
            AppLanguage.URDU to "مکمل نام"
        ),
        "phone_number" to mapOf(
            AppLanguage.ENGLISH to "Phone Number (e.g. 03001234567)",
            AppLanguage.URDU to "موبائل فون نمبر (مثلاً 03001234567)"
        ),
        "online_status" to mapOf(
            AppLanguage.ENGLISH to "Rider Availability Status",
            AppLanguage.URDU to "رائیڈر کی دستیابی کی کیفیت"
        ),
        "status_online" to mapOf(
            AppLanguage.ENGLISH to "Online (Receiving Orders)",
            AppLanguage.URDU to "آن لائن (آرڈرز کے لیے تیار)"
        ),
        "status_offline" to mapOf(
            AppLanguage.ENGLISH to "Offline (Resting)",
            AppLanguage.URDU to "آف لائن (آرام پر)"
        ),
        "error_generic" to mapOf(
            AppLanguage.ENGLISH to "An error occurred. Please check connection and try again.",
            AppLanguage.URDU to "ایک خرابی پیش آ گئی۔ براہِ کرم انٹرنیٹ چیک کر کے دوبارہ کوشش کریں۔"
        ),
        "nav_feedback" to mapOf(
            AppLanguage.ENGLISH to "Feedback",
            AppLanguage.URDU to "رائے اور فیڈ بیک"
        ),
        "active_orders_tab" to mapOf(
            AppLanguage.ENGLISH to "Active Orders",
            AppLanguage.URDU to "جاری آرڈرز"
        ),
        "history_tab" to mapOf(
            AppLanguage.ENGLISH to "Order History",
            AppLanguage.URDU to "آرڈر ہسٹری"
        ),
        "order_details_title" to mapOf(
            AppLanguage.ENGLISH to "Order Details",
            AppLanguage.URDU to "آرڈر کی تفصیلات"
        ),
        "final_bill_label" to mapOf(
            AppLanguage.ENGLISH to "Final Bill & Pricing",
            AppLanguage.URDU to "حتمی بل اور اخراجات"
        ),
        "delivery_completed_at" to mapOf(
            AppLanguage.ENGLISH to "Delivered At",
            AppLanguage.URDU to "ڈیلیوری کا وقت"
        ),
        "rider_info_label" to mapOf(
            AppLanguage.ENGLISH to "Assigned Rider",
            AppLanguage.URDU to "تفویض کردہ رائیڈر"
        ),
        "feedback_dialog_title" to mapOf(
            AppLanguage.ENGLISH to "Rate Delivery Service",
            AppLanguage.URDU to "سروس کی درجہ بندی کریں"
        ),
        "rating_label" to mapOf(
            AppLanguage.ENGLISH to "Rating (1 to 5 Stars)",
            AppLanguage.URDU to "ریٹنگ (1 تا 5 ستارے)"
        ),
        "comments_label" to mapOf(
            AppLanguage.ENGLISH to "Comments (Optional)",
            AppLanguage.URDU to "رائے یا تجاویز (اختیاری)"
        ),
        "btn_submit_feedback" to mapOf(
            AppLanguage.ENGLISH to "Submit Feedback",
            AppLanguage.URDU to "رائے ارسال کریں"
        ),
        "feedback_submitted_success" to mapOf(
            AppLanguage.ENGLISH to "Thank you! Your feedback has been recorded.",
            AppLanguage.URDU to "شکریہ! آپ کی قیمتی رائے ریکارڈ کر لی گئی ہے۔"
        ),
        "feedback_status_submitted" to mapOf(
            AppLanguage.ENGLISH to "Feedback Submitted",
            AppLanguage.URDU to "رائے درج ہو چکی ہے"
        ),
        "feedback_status_pending" to mapOf(
            AppLanguage.ENGLISH to "Leave Feedback",
            AppLanguage.URDU to "رائے درج کریں"
        ),
        "admin_tab_pending" to mapOf(
            AppLanguage.ENGLISH to "Pending Applications",
            AppLanguage.URDU to "زیرِ التوا درخواستیں"
        ),
        "admin_tab_approved" to mapOf(
            AppLanguage.ENGLISH to "Approved Riders",
            AppLanguage.URDU to "منظور شدہ رائیڈرز"
        ),
        "admin_tab_rejected" to mapOf(
            AppLanguage.ENGLISH to "Rejected Applications",
            AppLanguage.URDU to "مسترد شدہ درخواستیں"
        ),
        "cnic_front_label" to mapOf(
            AppLanguage.ENGLISH to "CNIC Front Image",
            AppLanguage.URDU to "شناختی کارڈ فرنٹ تصویر"
        ),
        "cnic_back_label" to mapOf(
            AppLanguage.ENGLISH to "CNIC Back Image",
            AppLanguage.URDU to "شناختی کارڈ بیک تصویر"
        ),
        "no_active_orders" to mapOf(
            AppLanguage.ENGLISH to "No active orders in progress.",
            AppLanguage.URDU to "فی الحال کوئی فعال آرڈر موجود نہیں ہے۔"
        ),
        "no_history_orders" to mapOf(
            AppLanguage.ENGLISH to "No completed or cancelled orders in history.",
            AppLanguage.URDU to "آرڈر ہسٹری میں کوئی مکمل یا منسوخ شدہ آرڈر نہیں ہے۔"
        ),
        "cat_delayed_delivery" to mapOf(
            AppLanguage.ENGLISH to "Delayed Delivery",
            AppLanguage.URDU to "تاخیر سے ترسیل"
        ),
        "cat_incorrect_items" to mapOf(
            AppLanguage.ENGLISH to "Incorrect Items",
            AppLanguage.URDU to "غلط اشیاء"
        ),
        "cat_bill_pricing_issue" to mapOf(
            AppLanguage.ENGLISH to "Bill or Pricing Issue",
            AppLanguage.URDU to "بل یا قیمت کا مسئلہ"
        ),
        "cat_rider_conduct" to mapOf(
            AppLanguage.ENGLISH to "Rider Conduct",
            AppLanguage.URDU to "رائیڈر کا رویہ"
        ),
        "cat_missing_items" to mapOf(
            AppLanguage.ENGLISH to "Missing Items",
            AppLanguage.URDU to "گمشدہ اشیاء"
        ),
        "cat_tech_issue" to mapOf(
            AppLanguage.ENGLISH to "Technical Issue",
            AppLanguage.URDU to "تکنیکی مسئلہ"
        ),
        "cat_other" to mapOf(
            AppLanguage.ENGLISH to "Other",
            AppLanguage.URDU to "دیگر"
        ),
        "complaint_status_open" to mapOf(
            AppLanguage.ENGLISH to "Open",
            AppLanguage.URDU to "کھلا ہے"
        ),
        "complaint_status_in_progress" to mapOf(
            AppLanguage.ENGLISH to "In Progress",
            AppLanguage.URDU to "زیرِ کارروائی"
        ),
        "complaint_status_resolved" to mapOf(
            AppLanguage.ENGLISH to "Resolved",
            AppLanguage.URDU to "حل شدہ"
        ),
        "help_center_title" to mapOf(
            AppLanguage.ENGLISH to "Complaints & Help Center",
            AppLanguage.URDU to "شکایات اور ہیلپ سینٹر"
        ),
        "dastak_hotline" to mapOf(
            AppLanguage.ENGLISH to "Dastak Hotline: 03049457463",
            AppLanguage.URDU to "دستک ہاٹ لائن: 03049457463"
        ),
        "call_hotline_btn" to mapOf(
            AppLanguage.ENGLISH to "Call Dastak Helpline",
            AppLanguage.URDU to "دستک ہیلپ لائن پر کال کریں"
        ),
        "select_order_optional" to mapOf(
            AppLanguage.ENGLISH to "Select Related Order (Optional)",
            AppLanguage.URDU to "متعلقہ آرڈر منتخب کریں (اختیاری)"
        ),
        "category_label" to mapOf(
            AppLanguage.ENGLISH to "Complaint Category",
            AppLanguage.URDU to "شکایت کا زمرہ"
        ),
        "admin_reply_label" to mapOf(
            AppLanguage.ENGLISH to "Administrator Response",
            AppLanguage.URDU to "انتظامیہ کا جواب"
        ),
        "no_complaints_history" to mapOf(
            AppLanguage.ENGLISH to "No complaints submitted yet.",
            AppLanguage.URDU to "ابھی تک کوئی شکایت درج نہیں کی گئی۔"
        ),
        "no_eligible_feedback_orders" to mapOf(
            AppLanguage.ENGLISH to "No delivered orders currently eligible for feedback.",
            AppLanguage.URDU to "فی الحال رائے دینے کے لیے کوئی ترسیل شدہ آرڈر موجود نہیں ہے۔"
        ),
        "admin_orders_title" to mapOf(
            AppLanguage.ENGLISH to "Orders Management",
            AppLanguage.URDU to "آرڈرز کا انتظام"
        ),
        "admin_complaints_title" to mapOf(
            AppLanguage.ENGLISH to "Complaints Management",
            AppLanguage.URDU to "شکایات کا انتظام"
        ),
        "admin_feedback_title" to mapOf(
            AppLanguage.ENGLISH to "Customer Feedback & Ratings",
            AppLanguage.URDU to "صارفین کی فیڈ بیک اور ریٹنگز"
        ),
        "confirm_location_title" to mapOf(
            AppLanguage.ENGLISH to "Confirm Your Delivery Location",
            AppLanguage.URDU to "اپنی ڈیلیوری لوکیشن کی تصدیق کریں"
        ),
        "btn_get_current_location" to mapOf(
            AppLanguage.ENGLISH to "Get My Current Location",
            AppLanguage.URDU to "میری موجودہ لوکیشن حاصل کریں"
        ),
        "btn_confirm_location_continue" to mapOf(
            AppLanguage.ENGLISH to "Confirm Location and Continue",
            AppLanguage.URDU to "لوکیشن کی تصدیق کرکے آگے بڑھیں"
        ),
        "location_permission_needed" to mapOf(
            AppLanguage.ENGLISH to "Location permission is required to detect your delivery address in Jhelum City Housing.",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ میں آپ کی ڈیلیوری لوکیشن جاننے کے لیے لوکیشن کی اجازت درکار ہے۔"
        ),
        "location_gps_unavailable" to mapOf(
            AppLanguage.ENGLISH to "GPS location is unavailable. Please turn on device location and retry.",
            AppLanguage.URDU to "جی پی ایس لوکیشن دستیاب نہیں ہے۔ برائے مہربانی ڈیوائس لوکیشن آن کرکے دوبارہ کوشش کریں۔"
        ),
        "btn_retry_gps" to mapOf(
            AppLanguage.ENGLISH to "Retry GPS",
            AppLanguage.URDU to "دوبارہ کوشش کریں"
        ),
        "route_calculating" to mapOf(
            AppLanguage.ENGLISH to "Calculating Road Route...",
            AppLanguage.URDU to "سڑک کے راستے کا فاصلہ معلوم کیا جا رہا ہے..."
        ),
        "route_error_title" to mapOf(
            AppLanguage.ENGLISH to "Route Calculation Failed",
            AppLanguage.URDU to "روٹ کا تعین نہ ہو سکا"
        ),
        "route_error_desc" to mapOf(
            AppLanguage.ENGLISH to "Unable to calculate road distance via OpenStreetMap routing. Please check your internet connection and retry.",
            AppLanguage.URDU to "اوپن اسٹریٹ میپ کے ذریعے روٹ کا فاصلہ معلوم نہ ہو سکا۔ برائے مہربانی انٹرنیٹ چیک کرکے دوبارہ کوشش کریں۔"
        ),
        "btn_retry_route" to mapOf(
            AppLanguage.ENGLISH to "Retry Route Calculation",
            AppLanguage.URDU to "دوبارہ روٹ معلوم کریں"
        ),
        "central_point_label" to mapOf(
            AppLanguage.ENGLISH to "Fixed Central Delivery Point (Origin)",
            AppLanguage.URDU to "مقررہ مرکزی ترسیلی مقام (آغاز)"
        ),
        "customer_pin_label" to mapOf(
            AppLanguage.ENGLISH to "Your Delivery Location (Destination)",
            AppLanguage.URDU to "آپ کا ڈیلیوری مقام (منزل)"
        ),
        "road_distance_label" to mapOf(
            AppLanguage.ENGLISH to "Road-Route Distance",
            AppLanguage.URDU to "سڑک کا فاصلہ"
        ),
        "delivery_fee_label" to mapOf(
            AppLanguage.ENGLISH to "Delivery Fee (PKR 70/km)",
            AppLanguage.URDU to "ڈیلیوری فیس (70 روپے فی کلومیٹر)"
        ),
        "service_fee_label" to mapOf(
            AppLanguage.ENGLISH to "Service Fee (5% / 7% under PKR 300)",
            AppLanguage.URDU to "سروس فیس (5٪ یا 7٪ تین سو سے کم پر)"
        ),
        "platform_fee_label" to mapOf(
            AppLanguage.ENGLISH to "Platform Fee",
            AppLanguage.URDU to "پلیٹ فارم فیس"
        ),
        "estimated_item_cost_label" to mapOf(
            AppLanguage.ENGLISH to "Estimated Item Cost (Optional)",
            AppLanguage.URDU to "سامان کی تخمینہ قیمت (اختیاری)"
        ),
        "estimated_total_label" to mapOf(
            AppLanguage.ENGLISH to "Estimated Total",
            AppLanguage.URDU to "تخمینہ شدہ کل رقم"
        ),
        "price_breakdown_title" to mapOf(
            AppLanguage.ENGLISH to "Complete Price Breakdown",
            AppLanguage.URDU to "قیمت کی مکمل تفصیلات"
        ),
        "payment_method_label" to mapOf(
            AppLanguage.ENGLISH to "Payment Method",
            AppLanguage.URDU to "طریقہ ادائیگی"
        ),
        "payment_cod" to mapOf(
            AppLanguage.ENGLISH to "Cash on Delivery",
            AppLanguage.URDU to "کیش آن ڈیلیوری (نقد ترسیل)"
        ),
        "payment_easypaisa" to mapOf(
            AppLanguage.ENGLISH to "EasyPaisa QR Code",
            AppLanguage.URDU to "ایزی پیسہ کیو آر کوڈ"
        ),
        "proceed_to_location_btn" to mapOf(
            AppLanguage.ENGLISH to "Set Delivery Location & View Pricing",
            AppLanguage.URDU to "ڈیلیوری لوکیشن اور کرایہ دیکھیں"
        ),
        "admin_ads_title" to mapOf(
            AppLanguage.ENGLISH to "Advertisements",
            AppLanguage.URDU to "اشتہارات"
        ),
        "ad_management_title" to mapOf(
            AppLanguage.ENGLISH to "Advertisement Management",
            AppLanguage.URDU to "اشتہارات کا انتظام"
        ),
        "ad_create_new" to mapOf(
            AppLanguage.ENGLISH to "Create New Advertisement",
            AppLanguage.URDU to "نیا اشتہار بنائیں"
        ),
        "ad_edit" to mapOf(
            AppLanguage.ENGLISH to "Edit Advertisement",
            AppLanguage.URDU to "اشتہار تبدیل کریں"
        ),
        "ad_preview" to mapOf(
            AppLanguage.ENGLISH to "Preview Advertisement",
            AppLanguage.URDU to "پیش منظر"
        ),
        "ad_title" to mapOf(
            AppLanguage.ENGLISH to "Ad Title",
            AppLanguage.URDU to "اشتہار کا عنوان"
        ),
        "ad_business_name" to mapOf(
            AppLanguage.ENGLISH to "Business / Advertiser Name",
            AppLanguage.URDU to "کاروبار یا مشتہر کا نام"
        ),
        "ad_category" to mapOf(
            AppLanguage.ENGLISH to "Category",
            AppLanguage.URDU to "کیٹیگری"
        ),
        "ad_description" to mapOf(
            AppLanguage.ENGLISH to "Description",
            AppLanguage.URDU to "تفصیل"
        ),
        "ad_offer" to mapOf(
            AppLanguage.ENGLISH to "Offer / Discount (Optional)",
            AppLanguage.URDU to "آفر یا رعایت (اختیاری)"
        ),
        "ad_price" to mapOf(
            AppLanguage.ENGLISH to "Price (PKR, Optional)",
            AppLanguage.URDU to "قیمت (روپے، اختیاری)"
        ),
        "ad_phone" to mapOf(
            AppLanguage.ENGLISH to "Contact Phone Number",
            AppLanguage.URDU to "رابطہ فون نمبر"
        ),
        "ad_media_type" to mapOf(
            AppLanguage.ENGLISH to "Media Type",
            AppLanguage.URDU to "میڈیا کی قسم"
        ),
        "ad_media_image" to mapOf(
            AppLanguage.ENGLISH to "Image",
            AppLanguage.URDU to "تصویر"
        ),
        "ad_media_video" to mapOf(
            AppLanguage.ENGLISH to "Short Video (≤60s)",
            AppLanguage.URDU to "مختصر ویڈیو (≤60 سیکنڈ)"
        ),
        "ad_media_source" to mapOf(
            AppLanguage.ENGLISH to "Media Source",
            AppLanguage.URDU to "میڈیا کا ذریعہ"
        ),
        "ad_upload_device" to mapOf(
            AppLanguage.ENGLISH to "Select Media from Device",
            AppLanguage.URDU to "ڈیوائس سے میڈیا منتخب کریں"
        ),
        "ad_media_url_input" to mapOf(
            AppLanguage.ENGLISH to "Media URL",
            AppLanguage.URDU to "میڈیا کا ویب لنک"
        ),
        "ad_poster_url" to mapOf(
            AppLanguage.ENGLISH to "Video Poster Image URL (Optional)",
            AppLanguage.URDU to "ویڈیو پوسٹر تصویر کا لنک (اختیاری)"
        ),
        "ad_status" to mapOf(
            AppLanguage.ENGLISH to "Status",
            AppLanguage.URDU to "حالت"
        ),
        "ad_status_published" to mapOf(
            AppLanguage.ENGLISH to "Published",
            AppLanguage.URDU to "شائع شدہ"
        ),
        "ad_status_draft" to mapOf(
            AppLanguage.ENGLISH to "Draft",
            AppLanguage.URDU to "مسودہ"
        ),
        "ad_status_paused" to mapOf(
            AppLanguage.ENGLISH to "Paused",
            AppLanguage.URDU to "معطل"
        ),
        "ad_status_archived" to mapOf(
            AppLanguage.ENGLISH to "Archived",
            AppLanguage.URDU to "محفوظ شدہ"
        ),
        "ad_active_toggle" to mapOf(
            AppLanguage.ENGLISH to "Active for Customers",
            AppLanguage.URDU to "کسٹمرز کے لیے فعال"
        ),
        "ad_start_date" to mapOf(
            AppLanguage.ENGLISH to "Start Date (Optional)",
            AppLanguage.URDU to "آغاز کی تاریخ (اختیاری)"
        ),
        "ad_end_date" to mapOf(
            AppLanguage.ENGLISH to "End Date (Optional)",
            AppLanguage.URDU to "اختتام کی تاریخ (اختیاری)"
        ),
        "ad_publish_now" to mapOf(
            AppLanguage.ENGLISH to "Publish Now",
            AppLanguage.URDU to "ابھی شائع کریں"
        ),
        "ad_save_draft" to mapOf(
            AppLanguage.ENGLISH to "Save as Draft",
            AppLanguage.URDU to "بطور مسودہ محفوظ کریں"
        ),
        "ad_save_changes" to mapOf(
            AppLanguage.ENGLISH to "Save Changes",
            AppLanguage.URDU to "تبدیلیاں محفوظ کریں"
        ),
        "ad_delete" to mapOf(
            AppLanguage.ENGLISH to "Delete Advertisement",
            AppLanguage.URDU to "اشتہار حذف کریں"
        ),
        "ad_delete_confirm" to mapOf(
            AppLanguage.ENGLISH to "Are you sure you want to delete this advertisement? This action cannot be undone.",
            AppLanguage.URDU to "کیا آپ واقعی اس اشتہار کو حذف کرنا چاہتے ہیں؟ یہ عمل واپس نہیں لیا جا سکتا۔"
        ),
        "ad_pause" to mapOf(
            AppLanguage.ENGLISH to "Pause",
            AppLanguage.URDU to "روکیں"
        ),
        "ad_activate" to mapOf(
            AppLanguage.ENGLISH to "Activate",
            AppLanguage.URDU to "فعال کریں"
        ),
        "ad_deactivate" to mapOf(
            AppLanguage.ENGLISH to "Deactivate",
            AppLanguage.URDU to "غیر فعال کریں"
        ),
        "ad_performance" to mapOf(
            AppLanguage.ENGLISH to "Performance",
            AppLanguage.URDU to "کارکردگی"
        ),
        "ad_views" to mapOf(
            AppLanguage.ENGLISH to "Views",
            AppLanguage.URDU to "مناظر"
        ),
        "ad_clicks" to mapOf(
            AppLanguage.ENGLISH to "Calls / Inquiries",
            AppLanguage.URDU to "رابطے / کالز"
        ),
        "ad_call_now" to mapOf(
            AppLanguage.ENGLISH to "Call Advertiser",
            AppLanguage.URDU to "رابطہ کریں"
        ),
        "ad_badge_featured" to mapOf(
            AppLanguage.ENGLISH to "Featured Offer",
            AppLanguage.URDU to "خصوصی پیشکش"
        ),
        "ad_no_active" to mapOf(
            AppLanguage.ENGLISH to "No active advertisements at this moment.",
            AppLanguage.URDU to "اس وقت کوئی فعال اشتہار موجود نہیں ہے۔"
        ),
        "ad_uploading" to mapOf(
            AppLanguage.ENGLISH to "Uploading media...",
            AppLanguage.URDU to "میڈیا اپلوڈ ہو رہا ہے..."
        ),
        "ad_upload_success" to mapOf(
            AppLanguage.ENGLISH to "Media uploaded successfully!",
            AppLanguage.URDU to "میڈیا کامیابی سے اپلوڈ ہو گیا!"
        ),
        "ad_upload_failed" to mapOf(
            AppLanguage.ENGLISH to "Media upload failed. You may also enter a direct URL.",
            AppLanguage.URDU to "میڈیا اپلوڈ ناکام ہو گیا۔ آپ براہ راست لنک بھی درج کر سکتے ہیں۔"
        ),
        "ad_validation_title" to mapOf(
            AppLanguage.ENGLISH to "Please provide an advertisement title.",
            AppLanguage.URDU to "برائے مہربانی اشتہار کا عنوان درج کریں۔"
        ),
        "ad_validation_desc" to mapOf(
            AppLanguage.ENGLISH to "Please enter a description for the advertisement.",
            AppLanguage.URDU to "برائے مہربانی اشتہار کی تفصیل درج کریں۔"
        ),
        "ad_validation_phone" to mapOf(
            AppLanguage.ENGLISH to "Please enter a valid contact phone number.",
            AppLanguage.URDU to "برائے مہربانی درست رابطہ نمبر درج کریں۔"
        ),
        "ad_validation_media" to mapOf(
            AppLanguage.ENGLISH to "Please select or enter media for this advertisement.",
            AppLanguage.URDU to "برائے مہربانی اشتہار کے لیے میڈیا منتخب کریں۔"
        ),
        "ad_video_too_large" to mapOf(
            AppLanguage.ENGLISH to "Video file exceeds maximum size (25MB limit).",
            AppLanguage.URDU to "ویڈیو فائل 25 ایم بی کی حد سے بڑی ہے۔"
        ),
        "ad_image_too_large" to mapOf(
            AppLanguage.ENGLISH to "Image file exceeds maximum size (5MB limit).",
            AppLanguage.URDU to "تصویر 5 ایم بی کی حد سے بڑی ہے۔"
        ),
        "ad_initial_promo_title" to mapOf(
            AppLanguage.ENGLISH to "Dastak Delivery Service",
            AppLanguage.URDU to "دستک ڈیلیوری سروس"
        ),
        "ad_initial_promo_desc" to mapOf(
            AppLanguage.ENGLISH to "Serving Jhelum City Housing with fast doorstep delivery of groceries, food, and essentials. App Coming Soon to Play Store!",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ میں سودا سلف، کھانا اور روزمرہ ضروریات کی تیز ترین ترسیل۔ ایپ جلد پلے اسٹور پر دستیاب ہوگی!"
        ),
        "ad_initial_promo_badge" to mapOf(
            AppLanguage.ENGLISH to "Jhelum City Housing",
            AppLanguage.URDU to "جہلم سٹی ہاؤسنگ"
        ),
        "ad_initial_promo_offer" to mapOf(
            AppLanguage.ENGLISH to "Fast & Reliable Neighborhood Delivery",
            AppLanguage.URDU to "فوری اور قابل اعتماد محلہ ڈیلیوری"
        ),
        "ad_coming_soon" to mapOf(
            AppLanguage.ENGLISH to "App Coming Soon",
            AppLanguage.URDU to "ایپ جلد آ رہی ہے"
        ),
        "rider_btn_arrived_outside" to mapOf(
            AppLanguage.ENGLISH to "I Have Arrived Outside",
            AppLanguage.URDU to "میں باہر پہنچ گیا ہوں"
        ),
        "rider_arrival_marked" to mapOf(
            AppLanguage.ENGLISH to "Arrival announced to customer",
            AppLanguage.URDU to "گاہک کو آمد کا صوتی اعلان بھیج دیا گیا ہے"
        ),
        "rider_arrived_outside_banner" to mapOf(
            AppLanguage.ENGLISH to "Rider has arrived outside your home!",
            AppLanguage.URDU to "آپ کا رائیڈر آپ کے گھر کے باہر پہنچ چکا ہے!"
        ),
        "replay_arrival_voice" to mapOf(
            AppLanguage.ENGLISH to "Play Voice Announcement",
            AppLanguage.URDU to "صوتی اعلان سنیں"
        ),
        "final_bill_notice" to mapOf(
            AppLanguage.ENGLISH to "Item prices and final total will be calculated after the rider purchases items and uploads the receipt. No advance bill is charged.",
            AppLanguage.URDU to "سامان کی قیمت اور حتمی بل رائیڈر کے دکان سے خریداری اور رسید اپ لوڈ کرنے کے بعد ظاہر ہوگا، جس کی تصدیق آپ کریں گے۔"
        ),
        "notification_permission_title" to mapOf(
            AppLanguage.ENGLISH to "Allow Arrival Notifications",
            AppLanguage.URDU to "آمد کی اطلاعات کی اجازت دیں"
        ),
        "notification_permission_desc" to mapOf(
            AppLanguage.ENGLISH to "Get instant spoken voice and high-priority alerts when your delivery rider arrives outside.",
            AppLanguage.URDU to "جب رائیڈر آپ کے گھر کے باہر پہنچے تو فوری صوتی اور اسکرین الرٹ حاصل کریں۔"
        )
    )

    val COMPLAINT_CATEGORIES = listOf(
        "cat_delayed_delivery",
        "cat_incorrect_items",
        "cat_bill_pricing_issue",
        "cat_rider_conduct",
        "cat_missing_items",
        "cat_tech_issue",
        "cat_other"
    )

    fun get(key: String, language: AppLanguage): String {
        val map = translations[key] ?: return key
        return map[language] ?: map[AppLanguage.ENGLISH] ?: key
    }

    fun getOrderStatusLabel(status: String, language: AppLanguage): String {
        return when (status) {
            "Pending" -> if (language == AppLanguage.URDU) "زیرِ التوا" else "Pending"
            "Assigned" -> if (language == AppLanguage.URDU) "رائیڈر تفویض شدہ" else "Rider Assigned"
            "Shopping" -> if (language == AppLanguage.URDU) "خریداری جاری ہے" else "Shopping for Items"
            "Awaiting Customer Bill Approval" -> if (language == AppLanguage.URDU) "بل منظوری کا انتظار" else "Bill Awaiting Approval"
            "Bill Rejected" -> if (language == AppLanguage.URDU) "بل مسترد" else "Bill Rejected"
            "Bill Approved" -> if (language == AppLanguage.URDU) "بل منظور شدہ" else "Bill Approved"
            "Out for Delivery" -> if (language == AppLanguage.URDU) "ڈیلیوری کے لیے روانہ" else "Out for Delivery"
            "Arrived Outside" -> if (language == AppLanguage.URDU) "گھر کے باہر پہنچ گیا" else "Arrived Outside"
            "Delivered" -> if (language == AppLanguage.URDU) "ترسیل مکمل" else "Delivered"
            "Cancelled" -> if (language == AppLanguage.URDU) "منسوخ" else "Cancelled"
            else -> status
        }
    }

    fun getComplaintStatusLabel(status: String, language: AppLanguage): String {
        return when (status.lowercase()) {
            "open" -> if (language == AppLanguage.URDU) "کھلا ہے" else "Open"
            "in_progress", "in progress", "in_review" -> if (language == AppLanguage.URDU) "زیرِ کارروائی" else "In Progress"
            "resolved" -> if (language == AppLanguage.URDU) "حل شدہ" else "Resolved"
            else -> status
        }
    }
}
