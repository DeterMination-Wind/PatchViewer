package patchviewer2;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.mod.DataPatcher;

import java.lang.reflect.Field;

final class PatchViewer2FieldCore{
    private static Object rootObject;
    private static ObjectMap<String, ContentType> typeMap;

    private PatchViewer2FieldCore(){
    }

    static Object rootObject(){
        if(rootObject == null){
            rootObject = Reflect.get(DataPatcher.class, "root");
        }
        return rootObject;
    }

    static ObjectMap<String, ContentType> typeMap(){
        if(typeMap == null){
            typeMap = Reflect.get(DataPatcher.class, "nameToType");
        }
        return typeMap;
    }

    static String contentKey(MappableContent content){
        return content.getContentType().name() + ":" + content.name;
    }

    static Seq<MappableContent> allMappableContents(){
        Seq<MappableContent> out = new Seq<MappableContent>();
        for(ContentType type : ContentType.all){
            Seq<?> seq = Vars.content.getBy(type);
            for(int i = 0; i < seq.size; i++){
                Object item = seq.get(i);
                if(item instanceof MappableContent){
                    out.add((MappableContent)item);
                }
            }
        }
        return out;
    }

    static OrderedMap<String, Object> flatFields(Object object, int maxDepth){
        OrderedMap<String, Object> out = new OrderedMap<String, Object>();
        collect(object, "", out, new ObjectSet<Object>(), 0, maxDepth);
        return out;
    }

    static Object resolvePath(Object root, String path){
        if(root == null || path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Object current = root;
        for(int i = 0; i < parts.length; i++){
            if(current == null) return null;
            Field field = findField(current.getClass(), parts[i]);
            if(field == null) return null;
            try{
                field.setAccessible(true);
                current = field.get(current);
            }catch(Throwable ignored){
                return null;
            }
        }
        return current;
    }

    static String stringifyValue(Object value){
        if(value == null) return null;
        Class<?> type = value.getClass();
        if(type.isArray()){
            int len = java.lang.reflect.Array.getLength(value);
            StringBuilder out = new StringBuilder();
            for(int i = 0; i < len; i++){
                Object child = java.lang.reflect.Array.get(value, i);
                String text = stringifyValue(child);
                if(text == null || text.isEmpty()) continue;
                if(out.length() > 0) out.append("  ");
                out.append(text);
            }
            return out.toString();
        }
        return String.valueOf(value);
    }

    private static void collect(Object object, String prefix, OrderedMap<String, Object> out, ObjectSet<Object> seen, int depth, int maxDepth){
        if(object == null || depth > maxDepth) return;
        Class<?> type = object.getClass();
        if(skipType(type)) return;
        if(!isSimple(type)){
            if(seen.contains(object)) return;
            seen.add(object);
        }

        Seq<Field> fields = fieldsOf(type);
        for(int i = 0; i < fields.size; i++){
            Field field = fields.get(i);
            Object value;
            try{
                field.setAccessible(true);
                value = field.get(object);
            }catch(Throwable ignored){
                continue;
            }
            String name = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            if(value == null){
                out.put(name, null);
                continue;
            }
            Class<?> valueType = value.getClass();
            if(isSimple(valueType)){
                out.put(name, value);
            }else if(depth < maxDepth){
                collect(value, name, out, seen, depth + 1, maxDepth);
            }
        }
    }

    private static Seq<Field> fieldsOf(Class<?> type){
        Seq<Field> out = new Seq<Field>();
        Class<?> current = type;
        while(current != null && current != Object.class){
            Field[] fields = current.getDeclaredFields();
            for(int i = 0; i < fields.length; i++){
                Field field = fields[i];
                if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                out.add(field);
            }
            current = current.getSuperclass();
        }
        return out;
    }

    private static Field findField(Class<?> type, String name){
        Class<?> current = type;
        while(current != null && current != Object.class){
            try{
                return current.getDeclaredField(name);
            }catch(NoSuchFieldException ignored){
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isSimple(Class<?> type){
        return type.isPrimitive()
            || Number.class.isAssignableFrom(type)
            || CharSequence.class.isAssignableFrom(type)
            || Boolean.class == type
            || Enum.class.isAssignableFrom(type)
            || type.isArray() && type.getComponentType() != null && (type.getComponentType().isPrimitive() || Number.class.isAssignableFrom(type.getComponentType()));
    }

    private static boolean skipType(Class<?> type){
        String name = type.getName();
        return name.startsWith("arc.graphics")
            || name.startsWith("arc.audio")
            || name.startsWith("java.lang.Class")
            || name.startsWith("mindustry.gen")
            || name.startsWith("mindustry.graphics.g3d");
    }
}
