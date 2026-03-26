# 1. حماية الكود الأساسي وتشفيره
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. حماية مكتبة Glide (المسؤولة عن الصور والتحميل)
-keep public class com.github.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep public class com.github.bumptech.glide.GeneratedGlideModuleImpl { *; }
-keep public class * extends com.github.bumptech.glide.module.AppGlideModule { *; }
-keep public class * extends com.github.bumptech.glide.module.LibraryGlideModule { *; }
-keep class com.github.bumptech.glide.load.resource.bitmap.VideoDecoder { *; }

# 3. حماية مكتبة Room (المسؤولة عن قاعدة البيانات)
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 4. حماية موديلات البيانات (Song, Album, etc.)
# ضروري جداً لضمان جلب النغمات من الهاتف دون أخطاء بعد التشفير
-keep class com.example.ringtoneplayer.models.** { *; }
-keepclassmembers class com.example.ringtoneplayer.models.** { *; }

# 5. حماية ViewBinding
-keep class com.example.ringtoneplayer.databinding.** { *; }

# 6. حماية مكتبة Gson (التعامل مع JSON)
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 7. منع حذف الدوال المرتبطة بالواجهات (XML onClick وغيرها)
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# 8. حماية الخدمات الخلفية (المشغل)
-keep class com.example.ringtoneplayer.services.MusicPlayerService { *; }
