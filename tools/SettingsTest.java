import megalodonte.engine.*;
import game.finalroz.Progress;
import java.nio.file.*;
import java.util.*;
class SettingsTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  var g=Codec.read(Files.readString(Path.of("game.json")));g.entrySceneId="options";var persisted=new HashMap<String,String>();
  var events=new megalodonte.engine.Runtime.Events(){public Map<String,String> loadState(){return persisted;}public void saveState(Map<String,String> state){persisted.clear();persisted.putAll(state);}};
  try(var r=new megalodonte.engine.Runtime(g,megalodonte.engine.Runtime.reflection(SettingsTest.class.getClassLoader()),events)){
   check(!r.scene.hud.stream().filter(h->h.id.equals("fps")).findFirst().orElseThrow().visible,"FPS default off");check(r.soundVolume==.35,"Default effects volume reduced");
   r.setAction("showFps",true);r.update(1./60,0,0);r.setAction("showFps",false);
   for(int i=0;i<60;i++)r.update(1./60,0,0);
   var fps=r.scene.hud.stream().filter(h->h.id.equals("fps")).findFirst().orElseThrow();check(fps.visible&&fps.text.equals("FPS: 60"),"FPS display enabled and real cadence");
   r.setAction("soundVolume",true);r.update(.01,0,0);r.setAction("soundVolume",false);check(r.soundVolume==.5,"Sound volume setting applied");
   var exported=Progress.portable(r.state);check(exported.get("showFps").equals("true")&&exported.get("soundVolume").equals("0.5"),"Settings in portable JSON");
   r.setAction("showFps",true);r.update(.01,0,0);r.setAction("showFps",false);check(!fps.visible,"FPS display disabled");
  }
  try(var r=new megalodonte.engine.Runtime(g,megalodonte.engine.Runtime.reflection(SettingsTest.class.getClassLoader()),events)){check(r.soundVolume==.5,"Volume restored on restart");check(r.state.get("showFps").equals("false"),"FPS restored on restart");}
  check(Progress.validate(Map.of("progress.unlocked","1")).get("soundVolume").equals("0.35"),"Old save compatibility");
  for(String value:new String[]{"NaN","-1","2"}){try{Progress.validate(Map.of("soundVolume",value));throw new AssertionError("Invalid gain accepted");}catch(IllegalArgumentException expected){}}
  System.out.println("PASS: FPS toggle, measured cadence, reduced effects gain, persistence and portable save compatibility");
 }
}
