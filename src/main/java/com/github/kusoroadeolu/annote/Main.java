import com.github.kusoroadeolu.annote.AnnoteRunner;
import com.github.kusoroadeolu.annote.Runner;
import com.github.kusoroadeolu.annote.TestClass;

void main() {
    Runner r = AnnoteRunner.newRunner(TestClass.class);
    r.run("fizzbuzz");  // 17 annotations to prove divisibility
//    r.run("call");      // Recursion: because one bad idea enables another
//    r.run("tryFields"); // State management via metadata
//    r.run("print");     // Your IDE's syntax highlighter is crying
//    r.run("repl");      // A calculator that shouldn't exist but does
}



