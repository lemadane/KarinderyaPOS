# Jackson (records are mapped via explicit @JsonProperty / @JsonCreator annotations)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Record
-keep class ph.karinderya.pos.data.model.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
