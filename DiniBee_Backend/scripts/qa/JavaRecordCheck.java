import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import javax.lang.model.element.Modifier;
import com.sun.source.tree.*;
import com.sun.source.util.*;
/** Bounded syntax-only check. Resolves explicit/single wildcard imports and unique type names. */
public class JavaRecordCheck {
 public static void main(String[] args) throws Exception {
  var compiler=ToolProvider.getSystemJavaCompiler();var files=new ArrayList<java.io.File>();
  try(var paths=Files.walk(Path.of(args[0]))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));}
  try(var fm=compiler.getStandardFileManager(null,null,null)) {
   var task=(JavacTask)compiler.getTask(null,fm,null,List.of("-proc:none","--release","17"),null,fm.getJavaFileObjectsFromFiles(files));
   var units=new ArrayList<CompilationUnitTree>();task.parse().forEach(units::add);
   var records=new HashMap<String,Integer>();var types=new HashMap<String,List<String>>();
   for(var unit:units)new TreeScanner<Void,String>(){
    @Override public Void visitClass(ClassTree t,String parent){var full=parent+"."+t.getSimpleName();types.computeIfAbsent(t.getSimpleName().toString(),x->new ArrayList<>()).add(full);
     if(t.getKind()==Tree.Kind.RECORD){int count=0;for(var m:t.getMembers())if(m instanceof VariableTree f&&!f.getModifiers().getFlags().contains(Modifier.STATIC))count++;records.put(full,count);}return super.visitClass(t,full);}
   }.scan(unit,unit.getPackageName().toString());
   int[] checked={0},skipped={0};
   for(var unit:units)new TreeScanner<Void,Void>(){
    @Override public Void visitNewClass(NewClassTree t,Void v){var type=t.getIdentifier() instanceof ParameterizedTypeTree p?p.getType().toString():t.getIdentifier().toString();var simple=type.substring(type.lastIndexOf('.')+1);String resolved=null;
     var candidates=new LinkedHashSet<String>();
     if(records.containsKey(type))candidates.add(type);
     if(records.containsKey(unit.getPackageName()+"."+type))candidates.add(unit.getPackageName()+"."+type);
     for(var i:unit.getImports()){var q=i.getQualifiedIdentifier().toString();var full=q.endsWith(".*")?q.substring(0,q.length()-1)+type:q.substring(0,q.lastIndexOf('.')+1)+type;if(records.containsKey(full))candidates.add(full);}
     if(candidates.isEmpty()&&type.contains("."))for(var name:records.keySet())if(name.endsWith("."+type))candidates.add(name);
     if(candidates.size()==1)resolved=candidates.iterator().next();
     else if(candidates.isEmpty()&&types.getOrDefault(simple,List.of()).size()==1&&records.containsKey(types.get(simple).get(0)))resolved=types.get(simple).get(0);
     if(resolved!=null){checked[0]++;if(t.getArguments().size()!=records.get(resolved))throw new AssertionError(unit.getSourceFile().getName()+": "+resolved+" expected "+records.get(resolved)+" args got "+t.getArguments().size());}
     else if(types.getOrDefault(simple,List.of()).stream().anyMatch(records::containsKey))skipped[0]++;
     return super.visitNewClass(t,v);}
   }.scan(unit,null);
   System.out.println("PASS: "+checked[0]+" record constructor calls checked; "+skipped[0]+" ambiguous names skipped. Argument counts only; no type compatibility checking.");
  }
 }
}
