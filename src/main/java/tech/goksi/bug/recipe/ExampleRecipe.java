package tech.goksi.bug.recipe;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.ClassDeclaration;

public class ExampleRecipe extends Recipe {

  @Override
  public @DisplayName @NotNull String getDisplayName() {
    return "Test";
  }

  @Override
  public @Description @NotNull String getDescription() {
    return "Test.";
  }

  @Override
  public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
    return Preconditions.check(
        new UsesType<>("tech.goksi.bug.util.ObjectMapperSupplier", true),
        new ExampleRecipeVisitor()
    );
  }

  private static class ExampleRecipeVisitor extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public @NotNull ClassDeclaration visitClassDeclaration(J.@NotNull ClassDeclaration classDecl,
        @NotNull ExecutionContext executionContext) {
      J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

      String methodToAdd = String.format("public ObjectMapper %s(ObjectMapperSupplier supplier) {"
          + "return supplier.get();"
          + "}", "getter");
      JavaTemplate template = JavaTemplate.builder(methodToAdd)
          .imports("com.fasterxml.jackson.databind.ObjectMapper",
              "tech.goksi.bug.util.ObjectMapperSupplier")
          .javaParser(
              JavaParser.fromJavaVersion().classpathFromResources(executionContext, "jackson-databind-2.17.3",
                  "rewrite-bug-reproduce-1.0-SNAPSHOT"))
          .build();

      cd = template.apply(getCursor(), cd.getBody().getCoordinates().lastStatement());
      updateCursor(cd);

      maybeAddImport("com.fasterxml.jackson.databind.ObjectMapper");
      maybeAddImport("tech.goksi.bug.util.ObjectMapperSupplier");
      return cd;
    }
  }
}
