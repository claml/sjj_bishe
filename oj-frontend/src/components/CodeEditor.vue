<template>
  <div
    class="code-editor"
    ref="codeEditorRef"
    :style="{ minHeight: minHeight, height }"
  />
</template>

<script setup lang="ts">
import * as monaco from "monaco-editor";
import {
  onBeforeUnmount,
  onMounted,
  ref,
  toRaw,
  watch,
  withDefaults,
  defineProps,
} from "vue";

interface Props {
  value: string;
  language?: string;
  handleChange?: (v: string) => void;
  readOnly?: boolean;
  height?: string;
  minHeight?: string;
  showMinimap?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  value: () => "",
  language: () => "java",
  handleChange: (v: string) => {
    console.log(v);
  },
  readOnly: () => false,
  height: () => "60vh",
  minHeight: () => "400px",
  showMinimap: () => true,
});

const codeEditorRef = ref();
const codeEditor = ref();

watch(
  () => props.language,
  () => {
    if (codeEditor.value) {
      monaco.editor.setModelLanguage(
        toRaw(codeEditor.value).getModel(),
        props.language
      );
    }
  }
);

watch(
  () => props.readOnly,
  (newValue) => {
    if (codeEditor.value) {
      toRaw(codeEditor.value).updateOptions({
        readOnly: newValue,
      });
    }
  }
);

watch(
  () => props.value,
  (newValue) => {
    if (!codeEditor.value) {
      return;
    }
    const editor = toRaw(codeEditor.value);
    const safeValue = newValue ?? "";
    if (safeValue !== editor.getValue()) {
      editor.setValue(safeValue);
    }
  }
);

onMounted(() => {
  if (!codeEditorRef.value) {
    return;
  }
  codeEditor.value = monaco.editor.create(codeEditorRef.value, {
    value: props.value,
    language: props.language,
    automaticLayout: true,
    colorDecorators: true,
    minimap: {
      enabled: props.showMinimap,
    },
    readOnly: props.readOnly,
    theme: "vs-dark",
  });

  codeEditor.value.onDidChangeModelContent(() => {
    props.handleChange(toRaw(codeEditor.value).getValue());
  });
});

onBeforeUnmount(() => {
  if (codeEditor.value) {
    toRaw(codeEditor.value).dispose();
  }
});
</script>

<style scoped></style>
