/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
import java.util.*;
public final class Progress {
    public static final int LEVELS=17;
    public static Map<String,String> validate(Map<String,String> values){
        var result=new LinkedHashMap<String,String>();
        for(var entry:values.entrySet()){
            String k=entry.getKey(),v=entry.getValue();
            if(k.equals("audio")||k.equals("touch")||k.equals("showFps")){if(!v.equals("true")&&!v.equals("false"))throw new IllegalArgumentException("Opção inválida: "+k);}
            else if(k.equals("soundVolume")){double volume=Double.parseDouble(v);if(!Double.isFinite(volume)||volume<0||volume>1)throw new IllegalArgumentException("Volume inválido");}
            else if(k.equals("progress.unlocked")){int n=Integer.parseInt(v);if(n<1||n>LEVELS)throw new IllegalArgumentException("Fase desbloqueada inválida");}
            else if(k.matches("best\\.level-(?:[1-9]|1[0-7])")){long ms=Long.parseLong(v);if(ms<0||ms>86400000)throw new IllegalArgumentException("Tempo inválido");}
            else throw new IllegalArgumentException("Propriedade desconhecida: "+k);
            result.put(k,v);
        }
        result.putIfAbsent("progress.unlocked","1");result.putIfAbsent("audio","true");result.putIfAbsent("touch","true");result.putIfAbsent("showFps","false");result.putIfAbsent("soundVolume","0.35");return result;
    }
    public static Map<String,String> portable(Map<String,String> state){var values=new LinkedHashMap<String,String>();for(var e:state.entrySet())if(e.getKey().equals("progress.unlocked")||e.getKey().equals("audio")||e.getKey().equals("touch")||e.getKey().equals("showFps")||e.getKey().equals("soundVolume")||e.getKey().startsWith("best."))values.put(e.getKey(),e.getValue());return validate(values);}
    static int unlocked(GameContext c){return Integer.parseInt(c.state().getOrDefault("progress.unlocked","1"));}
    static void complete(GameContext c,int level,double seconds){
        String key="best.level-"+level;long elapsed=Math.round(seconds*1000),old=Long.parseLong(c.state().getOrDefault(key,"86400000"));
        c.state().put(key,""+Math.min(old,elapsed));c.state().put("progress.unlocked",""+Math.max(unlocked(c),Math.min(LEVELS,level+1)));c.saveState();
    }
    static String time(String ms){if(ms==null)return "—";long t=Long.parseLong(ms);return String.format(Locale.ROOT,"%02d:%02d.%03d",t/60000,t/1000%60,t%1000);}
}
