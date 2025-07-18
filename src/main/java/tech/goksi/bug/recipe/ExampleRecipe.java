package tech.goksi.bug.recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.JavaType;

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

    private static final String PROCESSED_KEY = "tech.goksi.processed";
    private static final String TEMPLATE_KEY = "tech.goksi.template";

    @Override
    public @NotNull ClassDeclaration visitClassDeclaration(J.@NotNull ClassDeclaration classDecl,
        @NotNull ExecutionContext executionContext) {
      J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
      List<JavaTemplate> templates = getCursor().pollMessage(TEMPLATE_KEY);
      if (templates == null) {
        return cd;
      }

      for (JavaTemplate template : templates) {
        cd = template.apply(getCursor(), cd.getBody().getCoordinates().lastStatement());
        updateCursor(cd);
      }

      maybeAddImport("com.fasterxml.jackson.databind.ObjectMapper");
      maybeAddImport("tech.goksi.bug.util.ObjectMapperSupplier");
      return cd;
    }

    @Override
    public @NotNull MethodDeclaration visitMethodDeclaration(J.@NotNull MethodDeclaration method,
        @NotNull ExecutionContext ctx) {
      J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
      if (isProcessed(md, ctx)) {
        return md;
      }
      JavaType returnType = md.getMethodType().getReturnType();
      if (!(returnType instanceof JavaType.FullyQualified)) {
        return md;
      }

      JavaType.FullyQualified fq = (JavaType.FullyQualified) returnType;
      if (!"tech.goksi.bug.util.ObjectMapperSupplier".equals(fq.getFullyQualifiedName())) {
        return md;
      }

      String name = md.getSimpleName() + "Getter";

      String methodToAdd = String.format("public ObjectMapper %s(ObjectMapperSupplier supplier) {"
          + "return supplier.get();"
          + "}", name);
      JavaTemplate template = JavaTemplate.builder(methodToAdd)
          .imports("com.fasterxml.jackson.databind.ObjectMapper",
              "tech.goksi.bug.util.ObjectMapperSupplier")
          .javaParser(
              JavaParser.fromJavaVersion().classpathFromResources(ctx, "jackson-databind-2.17.3",
                  "rewrite-bug-reproduce-1.0-SNAPSHOT"))
          .build();
      List<JavaTemplate> templates = getCursor().getNearestMessage(TEMPLATE_KEY, new ArrayList<>());
      templates.add(template);
      getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, TEMPLATE_KEY, templates);

      setProcessed(md, ctx);

      return md;
    }

    private void setProcessed(J.MethodDeclaration md, ExecutionContext executionContext) {
      Set<UUID> processed = executionContext.getMessage(PROCESSED_KEY, new HashSet<>());
      processed.add(md.getId());
      executionContext.putMessage(PROCESSED_KEY, processed);
    }

    private boolean isProcessed(J.MethodDeclaration md, ExecutionContext executionContext) {
      Set<UUID> processed = executionContext.getMessage(PROCESSED_KEY, new HashSet<>());
      return processed.contains(md.getId());
    }
  }
}
