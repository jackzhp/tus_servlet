package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class ELevelSystem {

    String name;
    ArrayList<ELevel> levels = new ArrayList<>(); //always sorted.
    static ConcurrentHashMap<String, ELevelSystem> syss = new ConcurrentHashMap<>();

//levels can only be compared under the same level system.
    Comparator<ELevel> c = (ELevel l1, ELevel l2) -> {
        if (l1.sys != ELevelSystem.this || l2.sys != ELevelSystem.this) {
            throw new IllegalStateException("not of same system " + l1.sys.name + ":" + l2.sys.name);
        }
        int ret = l1.idMajor - l2.idMajor;
        if (ret == 0) {
            ret = l1.idMinor - l2.idMinor;
        }
        return ret;
    };

    ELevelSystem(String name) {
        this.name = name;
    }

    File getFile(boolean extnew) {
        String fn = name + ".json";
        if (extnew) {
            fn += ".new";
        }
        File f = new File(App.dirLevels(), fn);
        if (extnew) {
            if (f.exists()) {
                f.delete();
            }
        }
        return f;
    }

    void load() throws IOException {
        File f = getFile(false);
        if (f.exists() && f.length() > 0) {
        } else {
            return;
        }
        System.out.println("loading from " + f.getAbsolutePath());
        JsonParser p = App.getJSONparser(f);
        JsonToken t = p.nextToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    t = p.nextToken();
                    if ("name".equals(name)) {
                        String sysName = p.getValueAsString();
                        if (sysName.equals(this.name)) {
                        } else {
                            throw new IllegalStateException("level system name " + sysName + " is not expected " + this.name);
                        }
                    } else if ("levels".equals(name)) {
                        if (false) { //use array
                            ArrayList<ELevel> al = new ArrayList<>();
                            if (t == JsonToken.START_ARRAY) {
                                while (true) {
                                    t = p.nextToken();
                                    if (t == JsonToken.START_OBJECT) {
                                        ELevel level = new ELevel(this);
                                        level.parse(p);
                                        System.out.println("level parsed:" + level.levelString());
                                        if (level.idMajor == 0 || level.idMinor == 0) {
                                        } else {
                                            al.add(level);
                                        }
                                    } else if (t == JsonToken.END_ARRAY) {
                                        break;
                                    } else {
                                        throw new IllegalStateException("expecting end array, but " + t);
                                    }
                                }
                            } else {
                                throw new IllegalStateException("expecting start array, but " + t);
                            }
                            levels = al;
                        } else { //use object
                            parseLevels(p);
                        }
                    } else {
                        throw new IllegalStateException("unknown field name:" + name);
                    }
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                } else {
                    throw new IllegalStateException("expecting end object, but " + t);
                }
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
        System.out.println(name + " levels:" + levels.size());
    }

    void save() throws IOException {
        if (saving.compareAndSet(false, true)) {
            try {
                if (saveRequested >= saveLast) {
                    File fnew = getFile(true);
                    JsonGenerator g = App.getJSONgenerator(fnew);
                    g.writeStartObject();
                    g.writeStringField("name", name);
                    saveLast = System.currentTimeMillis();
                    saveNext = saveLast + saveDelayDefault;
                    if (false) {
                        g.writeArrayFieldStart("levels");
                        for (ELevel level : levels) {
                            level.json(g);
                        }
                        g.writeEndArray();
                    } else {
                        g.writeFieldName("levels");
                        jsonLevels(g);
                    }
                    g.writeEndObject();
                    g.flush();
                    g.close();
                    File f = getFile(false);
                    if (f.exists()) {
                        f.delete();
                    }
                    fnew.renameTo(f);
                }
            } finally {
                saving.set(false);
            }
        }
    }

    ELevel previousLevel(int major, int minor) {
        boolean found = false;
        for (int i = levels.size() - 1; i >= 0; i--) {
            ELevel level = levels.get(i);
            if (found) {
                return level;
            }
            if (major < level.idMajor) {
                continue;
            } else if (major == level.idMajor) {
                if (minor < level.idMinor) {
                    continue;
                } else if (minor == level.idMinor) {
                    found = true;
                } else {
                    return level;
                }
            } else {
                return level;
            }
        }
        return null;
    }

    ELevel nextLevel(ELevel actual) {
        boolean found = false;
        for (ELevel level : levels) {
            if (found) {
                return level;
            }
            if (level.equals(actual)) {
                found = true;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof ELevelSystem) {
                ELevelSystem s2 = (ELevelSystem) o;
                return name.equals(s2.name);
            }
        }
        return false;
    }

    static ELevelSystem getByName(String name) {
        try {
            ELevelSystem sys = syss.get(name); //it can deal with concurrent issue
            if (sys == null) {
                sys = new ELevelSystem(name);
                ELevelSystem sysO = syss.putIfAbsent(name, sys);
                if (sysO != null) {
                    sys = sysO;
                }
                sys.load();
            }
            return sys;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    ELevel getLevel_m(int major, int minor) {
        if (major == 0 || minor == 0) {
            throw new IllegalArgumentException(major + ":" + minor);
        }
        for (ELevel level : levels) {
            if (level.idMajor < major) {
                continue;
            } else if (level.idMajor == major) {
                if (level.idMinor < minor) {
                    continue;
                } else if (level.idMinor == minor) {
                    return level;
                } else {
                    break; //since the array is always sorted;
                }
            } else {
                break; //since the array is always well sorted 
            }
        }
        ELevel level = new ELevel(this);
        level.idMajor = major;
        level.idMinor = minor;
        levels.add(level);
        Collections.sort(levels, c);
        save(10);
        return level;
    }
    long saveRequested, saveLast, saveNext; //the next means the next saving must be after that.
    boolean nextScheduled;
    AtomicBoolean saving = new AtomicBoolean();
    private static long saveDelayDefault = 30; //seconds

    void save(long delay) { //this method is same for all 
        long ltsnow = System.currentTimeMillis();
        saveRequested = ltsnow;
        if (nextScheduled) {
        } else {
            long delay2 = saveNext - ltsnow;
            delay2 /= 1000; //now it is seconds
            delay2++;
            if (delay < delay2) {
                delay = delay2;
            }
            App.getExecutor().schedule(() -> {
                try {
                    nextScheduled = false;
                    save();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, delay, TimeUnit.SECONDS);
            nextScheduled = true;
        }
    }

    CompletableFuture<Boolean> save_cf() {
        CompletableFuture<Boolean> cf = new CompletableFuture<Boolean>();
        try {
            save();
            cf.complete(true);
        } catch (Throwable t) {
            cf.completeExceptionally(t);
        }
        return cf;
    }

    private void jsonLevels(JsonGenerator g) throws IOException {
        g.writeStartObject();
        for (ELevel level : levels) {
            g.writeFieldName(level.levelString());
            level.json(g);
        }
        g.writeEndObject();
    }

    private void parseLevels(JsonParser p) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String level_s = p.getCurrentName();
                    ELevel level = ELevel.get_m(this, level_s);
                    p.nextToken(); //I did this when I use array
                    level.parse(p);
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                } else {
                    throw new IllegalStateException("expecting end object, but " + t);
                }
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

}
