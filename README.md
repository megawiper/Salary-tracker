# Salary Tracker App

Daily wage tracker Android app. ₹580/din wage, Sunday default off, 1-15 aur 16-30/31 ke alag hisaab, payment date reminders (20 tarikh aur agle mahine ki 5 tarikh).

## GitHub par kaise chalayein aur APK banayein

1. Is poore folder ko naye GitHub repository mein upload/push karo.
2. Repo ke `Actions` tab mein jao — workflow apne aap chalega jab bhi `main` branch pe push hoga. Ya `Actions` tab se `Build APK` workflow ko manually `Run workflow` bhi kar sakte ho.
3. Build complete hone ke baad, us workflow run ke andar **Artifacts** section mein `salary-tracker-apk` milega — usko download karo, andar `app-debug.apk` hoga.
4. Wo APK apne Android phone mein transfer karke install kar lo (phone settings mein "unknown sources se install" allow karna padega, kyunki ye Play Store se nahi hai).

## App ka logic

- Daily wage: ₹580 (Settings icon se badal sakte ho)
- Har din pe tap karo: khaali → kaam kiya (green) → chutti (red) → khaali
- Sunday default "off" dikhega, paisa nahi judega. Agar Sunday ko kaam kiya to tap karo, green ho jayega aur paisa bhi judega
- 1-15 tarikh ka total alag dikhega, saath mein "paisa milega 20 tarikh ko"
- 16 se mahine ke aakhri din tak ka total alag dikhega, saath mein "paisa milega agle mahine ki 5 tarikh ko"
- **Jis din paisa milna hai (20 ya 5 tarikh) usi din top pe ek gold card dikhega: "Aaj paisa aana chahiye" saath mein final amount**
- **Har baar 20 ya 5 tarikh nikalne ke baad, us period ka final total History mein permanently save ho jata hai** (top bar ke history icon se dekh sakte ho) — purane mahino ka pura record wahan milega
- Sara data phone mein hi local database (Room/SQLite) mein save hota hai — internet ki zaroorat nahi

## Local development (agar Android Studio hai)

1. Is folder ko Android Studio mein `Open` karo
2. Gradle sync hone do
3. Run button dabao emulator ya connected phone par
