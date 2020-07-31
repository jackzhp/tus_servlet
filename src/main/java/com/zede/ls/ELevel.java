package com.zede.ls;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
//import javax.jdo.annotations.IdGeneratorStrategy;
//import javax.jdo.annotations.PersistenceCapable;
//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.PrimaryKey;

/**
 *
 * a system use a file to save info.
 *
 */
//@PersistenceCapable
public class ELevel //implements Comparable<ELevel> //use ELevelSystem.c instead.
{

//    @PrimaryKey
//    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    int id; //unique in all levels?
    /* neither 0 is used. unless for special purpose. if the user does not have any special purpose,
    then either 0 should render this ELevel to be ignored.
     */
    int idMajor, idMinor;
    ELevelSystem sys;

//    @Persistent(serialized="true")
//    Set<EKP> kps; //for this level, what are those EKP's
//    ArrayList<EKP> kps; //for this level, what are those EKP's
    HashSet<Integer> kps = new HashSet<>();
    HashSet<Integer> tests = new HashSet<>(); //for all the ETest's, 

    ELevel(ELevelSystem sys) {
        this.sys = sys;
    }

    /* in its system, if a test contains only EKPs belong to lower level, then the test will not be here.
instead, belong to the which which is the highest level of its EKP's.
     */
//    Set<ETest> tests; //do I really need this?
//    @Override
//    public int compareTo(ELevel o) {
//        int ret = idMajor - o.idMajor;
//        if (ret == 0) {
//            ret = idMinor - o.idMinor;
//        }
//        return ret;
//    }
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof ELevel) {
                ELevel l2 = (ELevel) o;
                if (sys.equals(l2.sys)) {
                    return sys.c.compare(this, l2) == 0;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return sys.hashCode() ^ idMajor ^ idMinor;
    }

    void jsonSimple(JsonGenerator g) throws IOException {
        g.writeStartObject();
        g.writeStringField("sys", sys.name);
        g.writeNumberField("major", idMajor);
        g.writeNumberField("minor", idMinor);
        g.writeEndObject();
    }

    static ELevel parseSimple(JsonParser p) throws IOException {
        JsonToken t = p.currentToken();//.nextToken(); //the caller has done the next..
        if (t == JsonToken.START_OBJECT) {
            String nameSys = null;
            int major = 0, minor = 0;
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();
                    p.nextToken();
                    if ("sys".equals(name)) {
                        nameSys = p.getValueAsString();
                    } else if ("major".equals(name)) {
                        major = p.getValueAsInt();// Integer.parseInt(value);
                    } else if ("minor".equals(name)) {
                        minor = p.getValueAsInt();// Integer.parseInt(value);
                    } else {
                        throw new IllegalStateException("unexpected field name:" + name);
                    }
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                } else {
                    throw new IllegalStateException("expecting end object, but " + t);
                }
            }
            if (nameSys != null) {
                ELevelSystem sys = ELevelSystem.getByName(nameSys);
                return sys.getLevel_m(major, minor);
            } else {
                throw new IllegalStateException("not system name");
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    void json(JsonGenerator g) throws IOException {
        g.writeStartObject();
//        g.writeStringField("sys", sys.name);
//        g.writeNumberField("major", idMajor);
//        g.writeNumberField("minor", idMinor);
        json(g, "kps", kps);
        json(g, "tests", tests);
        g.writeEndObject();
    }

    void parse(JsonParser p) throws IOException {
        JsonToken t = p.currentToken();// p.nextToken();  //the caller has to test this.
        if (t == JsonToken.START_OBJECT) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.FIELD_NAME) {
                    String name = p.getCurrentName();// p.nextFieldName();
                    p.nextToken();
                    if ("sys".equals(name)) {
                        //we can ignore the name
                    } else if ("major".equals(name)) {
                        idMajor = p.getValueAsInt();
                    } else if ("minor".equals(name)) {
                        idMinor = p.getValueAsInt();
                    } else if ("kps".equals(name)) {
                        parse(p, kps);
                    } else if ("tests".equals(name)) {
                        parse(p, tests);
                    } else {
                        throw new IllegalStateException("unknown field name:" + name);
                    }
                } else if (t == JsonToken.END_OBJECT) {
                    break;
                }
            }
        } else {
            throw new IllegalStateException("expecting start object, but " + t);
        }
    }

    private void parse(JsonParser p, HashSet<Integer> set) throws IOException {
//        if (idMinor == 15) {
//            System.out.println(this.levelString());
//        }
        JsonToken t = p.getCurrentToken();//.nextToken();
        if (t == JsonToken.START_ARRAY) {
            while (true) {
                t = p.nextToken();
                if (t == JsonToken.VALUE_NUMBER_INT) {
                    int id = p.getValueAsInt();
                    set.add(id);
                } else if (t == JsonToken.END_ARRAY) {
                    break;
                } else {
                    throw new IllegalStateException("expecting end array, but " + t);
                }
            }
        }
    }

    private void json(JsonGenerator g, String name, HashSet<Integer> kps) throws IOException {
        g.writeArrayFieldStart(name);
        for (Integer kpid : kps) {
            g.writeNumber(kpid);
        }
        g.writeEndArray();
    }

    String levelString() {
        return idMajor + "." + idMinor;
    }

    static ELevel get_m(String sysName, String ls) {
        return get_m(ELevelSystem.getByName(sysName), ls);
    }

    static ELevel get_m(ELevelSystem sys, String ls) {
        String[] as = ls.split("\\.");
        int[] mm = new int[2];
        for (int i = 0; i < 2; i++) {
            mm[i] = Integer.parseInt(as[i]);
        }
        return sys.getLevel_m(mm[0], mm[1]);
    }

    void replace(EKP remove, EKP keep) {
        if (remove.id == keep.id) {
            throw new IllegalStateException();
        }
        kps.remove(remove.id);
        kps.add(keep.id);
    }

    void addKP(int id) {
        kps.add(id);
        sys.save(20);
    }

    void removeKP(int id) {
        kps.remove(id);
        sys.save(20);
    }

    void removeTest(int id) {
        tests.remove(id);
        sys.save(20);
    }

    void addTest(int id) {
        tests.add(id);
        sys.save(20);
    }

    static class FunctionHalfEKP implements Function<ELevel, Boolean> {

//        private final ELevelSystem sys;
//        boolean repair; //should repair
//        //we need another flag to tell how to repair.
        int repair = App.FixHalf_Self; //1: do not touch the reciprocol(purge self). 2: do not touch self(fix the reciprocol)

        FunctionHalfEKP(int repair) { //ELevelSystem sys, 
//            this.sys = sys;
            if (repair != 0) {
                this.repair = repair;
            } else {
                //the default is used.
            }
        }

        @Override
        public Boolean apply(ELevel level) {
//            ELevel level = kp.getLevel(sys);
//            if (level != null) {
            if (level.kps.isEmpty()) {
                return false; //full relationship
            } else {
                int nhalf = 0;
                Integer[] a = level.kps.toArray(new Integer[0]);
                for (Integer kpid : a) {
                    try {
                        EKP kp = EKP.getByID_m(kpid, true);
                        if (kp.getLevel(level.sys) != level) {
                            nhalf++;
                            if (repair == App.FixHalf_Self) { //TODO: for this repair, I should log it for audio purpose: who and when did this.
                                level.kps.remove(kpid);
                            } else {
                                kp.set(level); //System.out.println("// ");
                            }
                        }
                    } catch (Throwable t) {
                        level.kps.remove(kpid);
                    }
                }
                return nhalf > 0;
            }
//            } else {
//                return false; //NOELevel
//            }
        }

    }

}
