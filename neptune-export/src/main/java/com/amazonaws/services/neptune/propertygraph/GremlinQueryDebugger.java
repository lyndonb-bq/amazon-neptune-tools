/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
*/

package com.amazonaws.services.neptune.propertygraph;

import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.tinkerpop.gremlin.process.traversal.*;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.TraversalStrategyProxy;
import org.apache.tinkerpop.gremlin.process.traversal.util.ConnectiveP;
import org.apache.tinkerpop.gremlin.process.traversal.util.OrP;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.util.function.Lambda;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class GremlinQueryDebugger {

    public static String queryAsString(Object o){
        return String.valueOf(new DefaultTypeTranslator().apply("g", o));
    }


    public static class DefaultTypeTranslator implements Translator.ScriptTranslator.TypeTranslator {
        public DefaultTypeTranslator() {
        }

        public Object apply(final String traversalSource, final Object o) {
            return o instanceof Bytecode ? this.internalTranslate(traversalSource, (Bytecode)o) : this.convertToString(o);
        }

        protected String convertToString(final Object object) {
            if (object instanceof Bytecode.Binding) {
                return ((Bytecode.Binding)object).variable();
            } else if (object instanceof Bytecode) {
                return this.internalTranslate("__", (Bytecode)object);
            } else if (object instanceof Traversal) {
                return this.convertToString(((Traversal)object).asAdmin().getBytecode());
            } else if (object instanceof String) {
                return (((String)object).contains("\"") ? "\"\"\"" + StringEscapeUtils.escapeJava((String)object) + "\"\"\"" : "\"" + StringEscapeUtils.escapeJava((String)object) + "\"").replace("$", "\\$");
            } else {
                Iterator var3;
                Object item;
                if (object instanceof Set) {
                    Set<String> set = new HashSet(((Set)object).size());
                    var3 = ((Set)object).iterator();

                    while(var3.hasNext()) {
                        item = var3.next();
                        set.add(this.convertToString(item));
                    }

                    return set.toString() + " as Set";
                } else if (object instanceof List) {
                    List<String> list = new ArrayList(((List)object).size());
                    var3 = ((List)object).iterator();

                    while(var3.hasNext()) {
                        item = var3.next();
                        list.add(this.convertToString(item));
                    }

                    return list.toString();
                } else if (!(object instanceof Map)) {
                    if (object instanceof Long) {
                        return object + "L";
                    } else if (object instanceof Double) {
                        return object + "d";
                    } else if (object instanceof Float) {
                        return object + "f";
                    } else if (object instanceof Integer) {
                        return "(int) " + object;
                    } else if (object instanceof Class) {
                        return ((Class)object).getCanonicalName();
                    } else if (object instanceof Timestamp) {
                        return "new java.sql.Timestamp(" + ((Timestamp)object).getTime() + ")";
                    } else if (object instanceof Date) {
                        return "new java.util.Date(" + ((Date)object).getTime() + ")";
                    } else if (object instanceof UUID) {
                        return "java.util.UUID.fromString('" + object.toString() + "')";
                    } else if (object instanceof P) {
                        return this.convertPToString((P)object, new StringBuilder()).toString();
                    } else if (object instanceof SackFunctions.Barrier) {
                        return "SackFunctions.Barrier." + object.toString();
                    } else if (object instanceof VertexProperty.Cardinality) {
                        return "VertexProperty.Cardinality." + object.toString();
                    } else if (object instanceof TraversalOptionParent.Pick) {
                        return "TraversalOptionParent.Pick." + object.toString();
                    } else if (object instanceof Enum) {
                        return ((Enum)object).getDeclaringClass().getSimpleName() + "." + object.toString();
                    } else if (object instanceof Element) {
                        if (object instanceof Vertex) {
                            Vertex vertex = (Vertex)object;
                            return "new org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex(" + this.convertToString(vertex.id()) + "," + this.convertToString(vertex.label()) + ", Collections.emptyMap())";
                        } else if (object instanceof Edge) {
                            Edge edge = (Edge)object;
                            return "new org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge(" + this.convertToString(edge.id()) + "," + this.convertToString(edge.label()) + ",Collections.emptyMap()," + this.convertToString(edge.outVertex().id()) + "," + this.convertToString(edge.outVertex().label()) + "," + this.convertToString(edge.inVertex().id()) + "," + this.convertToString(edge.inVertex().label()) + ")";
                        } else {
                            VertexProperty vertexProperty = (VertexProperty)object;
                            return "new org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty(" + this.convertToString(vertexProperty.id()) + "," + this.convertToString(vertexProperty.label()) + "," + this.convertToString(vertexProperty.value()) + ",Collections.emptyMap()," + this.convertToString(vertexProperty.element()) + ")";
                        }
                    } else if (object instanceof Lambda) {
                        String lambdaString = ((Lambda)object).getLambdaScript().trim();
                        return lambdaString.startsWith("{") ? lambdaString : "{" + lambdaString + "}";
                    } else if (object instanceof TraversalStrategyProxy) {
                        TraversalStrategyProxy proxy = (TraversalStrategyProxy)object;
                        return proxy.getConfiguration().isEmpty() ? proxy.getStrategyClass().getCanonicalName() + ".instance()" : proxy.getStrategyClass().getCanonicalName() + ".create(new org.apache.commons.configuration.MapConfiguration(" + this.convertToString(ConfigurationConverter.getMap(proxy.getConfiguration())) + "))";
                    } else if (object instanceof TraversalStrategy) {
                        return this.convertToString(new TraversalStrategyProxy((TraversalStrategy)object));
                    } else {
                        return null == object ? "null" : object.toString();
                    }
                } else {
                    StringBuilder map = new StringBuilder("[");
                    var3 = ((Map)object).entrySet().iterator();

                    while(var3.hasNext()) {
                        Map.Entry<?, ?> entry = (Map.Entry)var3.next();
                        map.append("(").append(this.convertToString(entry.getKey())).append("):(").append(this.convertToString(entry.getValue())).append("),");
                    }

                    if (!((Map)object).isEmpty()) {
                        map.deleteCharAt(map.length() - 1);
                    }

                    return map.append("]").toString();
                }
            }
        }

        protected String internalTranslate(final String start, final Bytecode bytecode) {
            StringBuilder traversalScript = new StringBuilder(start);
            Iterator var4 = bytecode.getInstructions().iterator();

            while(true) {
                while(var4.hasNext()) {
                    Bytecode.Instruction instruction = (Bytecode.Instruction)var4.next();
                    String methodName = instruction.getOperator();
                    if (0 == instruction.getArguments().length) {
                        traversalScript.append(".").append(methodName).append("()");
                    } else {
                        traversalScript.append(".");
                        String temp = methodName + "(";
                        if (methodName.equals("withSack") && instruction.getArguments().length == 2 && instruction.getArguments()[1] instanceof Lambda) {
                            String castFirstArgTo = instruction.getArguments()[0] instanceof Lambda ? Supplier.class.getName() : "";
                            Lambda secondArg = (Lambda)instruction.getArguments()[1];
                            String castSecondArgTo = secondArg.getLambdaArguments() == 1 ? UnaryOperator.class.getName() : BinaryOperator.class.getName();
                            if (!castFirstArgTo.isEmpty()) {
                                temp = temp + String.format("(%s) ", castFirstArgTo);
                            }

                            temp = temp + String.format("%s, (%s) %s,", this.convertToString(instruction.getArguments()[0]), castSecondArgTo, this.convertToString(instruction.getArguments()[1]));
                        } else {
                            Object[] var8 = instruction.getArguments();
                            int var9 = var8.length;

                            for(int var10 = 0; var10 < var9; ++var10) {
                                Object object = var8[var10];
                                temp = temp + this.convertToString(object) + ",";
                            }
                        }

                        traversalScript.append(temp.substring(0, temp.length() - 1)).append(")");
                    }
                }

                return traversalScript.toString();
            }
        }

        protected StringBuilder convertPToString(final P p, final StringBuilder current) {
            if (p instanceof TextP) {
                return this.convertTextPToString((TextP)p, current);
            } else {
                if (p instanceof ConnectiveP) {
                    List<P<?>> list = ((ConnectiveP)p).getPredicates();

                    for(int i = 0; i < list.size(); ++i) {
                        this.convertPToString((P)list.get(i), current);
                        if (i < list.size() - 1) {
                            current.append(p instanceof OrP ? ".or(" : ".and(");
                        }
                    }

                    current.append(")");
                } else {
                    current.append("P.").append(p.getBiPredicate().toString()).append("(").append(this.convertToString(p.getValue())).append(")");
                }

                return current;
            }
        }

        protected StringBuilder convertTextPToString(final TextP p, final StringBuilder current) {
            current.append("TextP.").append(p.getBiPredicate().toString()).append("(").append(this.convertToString(p.getValue())).append(")");
            return current;
        }
    }



    public static class StringEscapeUtils {
        public StringEscapeUtils() {
        }

        public static String escapeJava(String str) {
            return escapeJavaStyleString(str, false, false);
        }

        public static void escapeJava(Writer out, String str) throws IOException {
            escapeJavaStyleString(out, str, false, false);
        }

        public static String escapeJavaScript(String str) {
            return escapeJavaStyleString(str, true, true);
        }

        public static void escapeJavaScript(Writer out, String str) throws IOException {
            escapeJavaStyleString(out, str, true, true);
        }

        private static String escapeJavaStyleString(String str, boolean escapeSingleQuotes, boolean escapeForwardSlash) {
            if (str == null) {
                return null;
            } else {
                try {
                    Writer writer = new StringBuilderWriter(str.length() * 2);
                    escapeJavaStyleString(writer, str, escapeSingleQuotes, escapeForwardSlash);
                    return writer.toString();
                } catch (IOException var4) {
                    throw new RuntimeException(var4);
                }
            }
        }

        private static void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote, boolean escapeForwardSlash) throws IOException {
            if (out == null) {
                throw new IllegalArgumentException("The Writer must not be null");
            } else if (str != null) {
                int sz = str.length();

                for(int i = 0; i < sz; ++i) {
                    char ch = str.charAt(i);
                    if (ch > 4095) {
                        out.write("\\u" + hex(ch));
                    } else if (ch > 255) {
                        out.write("\\u0" + hex(ch));
                    } else if (ch > 127) {
                        out.write("\\u00" + hex(ch));
                    } else if (ch < ' ') {
                        switch(ch) {
                            case '\b':
                                out.write(92);
                                out.write(98);
                                break;
                            case '\t':
                                out.write(92);
                                out.write(116);
                                break;
                            case '\n':
                                out.write(92);
                                out.write(110);
                                break;
                            case '\u000b':
                            default:
                                if (ch > 15) {
                                    out.write("\\u00" + hex(ch));
                                } else {
                                    out.write("\\u000" + hex(ch));
                                }
                                break;
                            case '\f':
                                out.write(92);
                                out.write(102);
                                break;
                            case '\r':
                                out.write(92);
                                out.write(114);
                        }
                    } else {
                        switch(ch) {
                            case '"':
                                out.write(92);
                                out.write(34);
                                break;
                            case '\'':
                                if (escapeSingleQuote) {
                                    out.write(92);
                                }

                                out.write(39);
                                break;
                            case '/':
                                if (escapeForwardSlash) {
                                    out.write(92);
                                }

                                out.write(47);
                                break;
                            case '\\':
                                out.write(92);
                                out.write(92);
                                break;
                            default:
                                out.write(ch);
                        }
                    }
                }

            }
        }

        private static String hex(char ch) {
            return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
        }

        public static String unescapeJava(String str) {
            if (str == null) {
                return null;
            } else {
                try {
                    Writer writer = new StringBuilderWriter(str.length());
                    unescapeJava(writer, str);
                    return writer.toString();
                } catch (IOException var2) {
                    throw new RuntimeException(var2);
                }
            }
        }

        public static void unescapeJava(Writer out, String str) throws IOException {
            if (out == null) {
                throw new IllegalArgumentException("The Writer must not be null");
            } else if (str != null) {
                int sz = str.length();
                StringBuilder unicode = new StringBuilder(4);
                boolean hadSlash = false;
                boolean inUnicode = false;

                for(int i = 0; i < sz; ++i) {
                    char ch = str.charAt(i);
                    if (inUnicode) {
                        unicode.append(ch);
                        if (unicode.length() == 4) {
                            try {
                                int value = Integer.parseInt(unicode.toString(), 16);
                                out.write((char)value);
                                unicode.setLength(0);
                                inUnicode = false;
                                hadSlash = false;
                            } catch (NumberFormatException var9) {
                                throw new RuntimeException("Unable to parse unicode value: " + unicode, var9);
                            }
                        }
                    } else if (hadSlash) {
                        hadSlash = false;
                        switch(ch) {
                            case '"':
                                out.write(34);
                                break;
                            case '\'':
                                out.write(39);
                                break;
                            case '\\':
                                out.write(92);
                                break;
                            case 'b':
                                out.write(8);
                                break;
                            case 'f':
                                out.write(12);
                                break;
                            case 'n':
                                out.write(10);
                                break;
                            case 'r':
                                out.write(13);
                                break;
                            case 't':
                                out.write(9);
                                break;
                            case 'u':
                                inUnicode = true;
                                break;
                            default:
                                out.write(ch);
                        }
                    } else if (ch == '\\') {
                        hadSlash = true;
                    } else {
                        out.write(ch);
                    }
                }

                if (hadSlash) {
                    out.write(92);
                }

            }
        }

        public static String unescapeJavaScript(String str) {
            return unescapeJava(str);
        }

        public static void unescapeJavaScript(Writer out, String str) throws IOException {
            unescapeJava(out, str);
        }
    }


    public static class StringBuilderWriter extends Writer implements Serializable {
        private static final long serialVersionUID = -146927496096066153L;
        private final StringBuilder builder;

        public StringBuilderWriter() {
            this.builder = new StringBuilder();
        }

        public StringBuilderWriter(int capacity) {
            this.builder = new StringBuilder(capacity);
        }

        public StringBuilderWriter(StringBuilder builder) {
            this.builder = builder != null ? builder : new StringBuilder();
        }

        public Writer append(char value) {
            this.builder.append(value);
            return this;
        }

        public Writer append(CharSequence value) {
            this.builder.append(value);
            return this;
        }

        public Writer append(CharSequence value, int start, int end) {
            this.builder.append(value, start, end);
            return this;
        }

        public void close() {
        }

        public void flush() {
        }

        public void write(String value) {
            if (value != null) {
                this.builder.append(value);
            }

        }

        public void write(char[] value, int offset, int length) {
            if (value != null) {
                this.builder.append(value, offset, length);
            }

        }

        public StringBuilder getBuilder() {
            return this.builder;
        }

        public String toString() {
            return this.builder.toString();
        }
    }
}
