import megalodonte.engine.*;
import java.nio.file.*;
public class MemoryTest {
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);Game manifest;
  try(var reader=Files.newBufferedReader(root.resolve("game.json"))){manifest=Codec.read(reader);}
  int count=0;long peak=0;
  for(var descriptor:manifest.scenes){
   manifest.entrySceneId=descriptor.id;
   try(var runtime=new megalodonte.engine.Runtime(manifest,megalodonte.engine.Runtime.reflection(MemoryTest.class.getClassLoader()),new megalodonte.engine.Runtime.Events(){},id->{try(var reader=Files.newBufferedReader(root.resolve(manifest.sceneFiles.get(id)))){return Codec.readScene(reader);}})){
    runtime.update(.016,0,0);count++;System.gc();var vm=java.lang.Runtime.getRuntime();peak=Math.max(peak,vm.totalMemory()-vm.freeMemory());
   }
  }
  System.out.println("Validated "+count+" scenes, peak live Java heap="+peak/1048576+" MiB, heap limit="+java.lang.Runtime.getRuntime().maxMemory()/1048576+" MiB");
 }
}
