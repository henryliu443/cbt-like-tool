sed -i '' -e '/val isDeepReasoningModel: Boolean/,/val isGeminiProModel: Boolean/{
  d
}' shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/ReframeViewModel.kt

sed -i '' -e '/get() {/,/}/d' shared/src/commonMain/kotlin/com/henryliu/cbtreframe/shared/ReframeViewModel.kt

# Wait, this is getting complicated with sed. Let's just write a python script to do the replacement.
