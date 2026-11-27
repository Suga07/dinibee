import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;

public class JavaSyntaxCheck {
    public static void main(String[] args) throws Exception {
        var compiler=ToolProvider.getSystemJavaCompiler();
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        List<Path> sources;
        try(var paths=Files.walk(Path.of(args[0]))) { sources=paths.filter(p -> p.toString().endsWith(".java")).toList(); }
        try(var manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8)) {
            var task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none","--release","17"),null,manager.getJavaFileObjectsFromPaths(sources));
            int count=0;
            for(var unit:task.parse()) count++;
            var errors=diagnostics.getDiagnostics().stream().filter(d -> d.getKind()==Diagnostic.Kind.ERROR).toList();
            for(var error:errors) System.out.println(error);
            if(!errors.isEmpty()) throw new AssertionError("Java syntax errors: "+errors.size());
            System.out.println("Java 17 syntax parse passed: "+count+" source files (dependency type-checking not performed)");
        }
    }
}
