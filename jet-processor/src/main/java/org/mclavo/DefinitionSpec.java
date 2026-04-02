package org.mclavo;

import java.util.List;

/**
 * Immutable metadata used to generate a {@code BeanDefinition} class.
 * <p>
 * 
 *  @param packageName The package for the generated class.</li>
 *  @param imports The required imports for the generated source.</li>
 *  @param simpleClassName The simple name of the generated class.</li>
 *  @param beanType The fully qualified type of the bean represented by this definition.</li>
 *  @param qualifierExpression: The qualifier expression used by {@code BeanKey}.</li>
 *  @param primary Whether this bean is marked as primary.</li>
 *  @param creationExpression The expression used to create the bean instance.</li>
 * 
 */
final class DefinitionSpec {
    private final String packageName;
    private final List<String> imports;
    private final String simpleClassName;
    private final String beanType;
    private final String qualifierExpression;
    private final boolean primary;
    private final String creationExpression;

    private DefinitionSpec(Builder builder) {
        this.packageName = builder.packageName;
        this.imports = builder.imports;
        this.simpleClassName = builder.simpleClassName;
        this.beanType = builder.beanType;
        this.qualifierExpression = builder.qualifierExpression;
        this.primary = builder.primary;
        this.creationExpression = builder.creationExpression;
    }

    String getPackageName() {
        return packageName;
    }

    List<String> getImports() {
        return imports;
    }

    String getSimpleClassName() {
        return simpleClassName;
    }

    String getBeanType() {
        return beanType;
    }

    String getQualifierExpression() {
        return qualifierExpression;
    }

    boolean isPrimary() {
        return primary;
    }

    String getCreationExpression() {
        return creationExpression;
    }

    public static final class Builder {
        private String packageName;
        private List<String> imports;
        private String simpleClassName;
        private String beanType;
        private String qualifierExpression;
        private boolean primary;
        private String creationExpression;

        Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        Builder imports(List<String> imports) {
            this.imports = imports;
            return this;
        }

        Builder simpleClassName(String simpleClassName) {
            this.simpleClassName = simpleClassName;
            return this;
        }

        Builder beanType(String beanType) {
            this.beanType = beanType;
            return this;
        }

        Builder qualifierExpression(String qualifierExpression) {
            this.qualifierExpression = qualifierExpression;
            return this;
        }

        Builder primary(boolean primary) {
            this.primary = primary;
            return this;
        }

        Builder creationExpression(String creationExpression) {
            this.creationExpression = creationExpression;
            return this;
        }

        DefinitionSpec build() {
            return new DefinitionSpec(this);
        }
    }
}