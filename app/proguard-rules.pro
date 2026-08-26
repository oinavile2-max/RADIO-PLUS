# A versão release usa o otimizador padrão do Android, R8 e remoção de recursos.
# Não preservar indiscriminadamente nomes de classes: isso reduziria a proteção.
# Regras keep devem ser adicionadas somente para bibliotecas que realmente usam
# reflexão/JNI e apenas no menor escopo necessário.

-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
