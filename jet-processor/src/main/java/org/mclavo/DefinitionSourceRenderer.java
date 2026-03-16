package org.mclavo;

import java.util.stream.Collectors;

public final class DefinitionSourceRenderer {

    private static String DefinitionTemplate = 
    """
    package %s;

    %s

    public final class %s implements BeanDefinition<%s> {

        private final ScopeProvider<%s> provider =
            ScopeProvider.singletonScope(beanProvider -> %s);

        @Override
        public %s apply(BeanProvider beanProvider) {
            return provider.apply(beanProvider);
        }

        @Override
        public BeanKey<%s> key() {
            return BeanKey.of(%s.class, %s);
        }
    }
        
    """;
    

    public static String render(DefinitionSpec spec) {
        String importsBlock = spec.imports().stream()
            .sorted()
            .map(importName -> "import " + importName + ";")
            .collect(Collectors.joining("\n"));
        
        return DefinitionTemplate.formatted(
            spec.packageName(),
            importsBlock,
            spec.simpleClassName(),
            spec.beanType(),
            spec.beanType(),
            spec.creationExpression(),
            spec.beanType(),
            spec.beanType(),
            spec.beanType(),
            spec.qualifierExpression()
        );

    }


}
