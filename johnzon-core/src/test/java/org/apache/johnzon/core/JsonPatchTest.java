/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.core;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class JsonPatchTest {

    @Test
    public void testAddObjectMember() {

        JsonObject object = Json.createReader(new StringReader("{ \"foo\": \"bar\" }"))
                                .readObject();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/baz"),
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals("bar", patched.getString("foo"));
        assertEquals("qux", patched.getString("baz"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"qux\"}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementWithIndex() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/foo/1"),
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("bar", array.getString(0));
        assertEquals("qux", array.getString(1));
        assertEquals("baz", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"qux\",\"baz\"]}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementAppend() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/foo/-"),
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));
        assertEquals("qux", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"baz\",\"qux\"]}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementPlainArray() {
        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("baz")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/-"),
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals("bar", patched.getString(0));
        assertEquals("baz", patched.getString(1));
        assertEquals("qux", patched.getString(2));

        assertEquals("[\"bar\",\"baz\",\"qux\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testAddNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/baz/bat"),
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testAddArrayIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/5"),
                                                                             null,
                                                                             new JsonStringImpl("baz")));

        patch.apply(array);
    }


    @Test
    public void testRemoveObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("baz", "qux")
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/baz"),
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals("bar", patched.getString("foo"));
        assertFalse("patched JsonObject must no contain \"baz\"", patched.containsKey("baz"));

        assertEquals("{\"foo\":\"bar\"}", toJsonString(patched));
    }

    @Test
    public void testRemoveArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("qux")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/foo/1"),
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals(2, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));

        assertEquals("{\"foo\":[\"bar\",\"baz\"]}", toJsonString(patched));
    }

    @Test
    public void testRemoveArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("qux")
                              .add("baz")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/1"),
                                                                             null,
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertEquals(2, patched.size());
        assertEquals("bar", patched.getString(0));
        assertEquals("baz", patched.getString(1));

        assertEquals("[\"bar\",\"baz\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testRemoveObjectElementNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .add("baz", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/nomatch"),
                                                                             null,
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/5"),
                                                                             null,
                                                                             null));

        patch.apply(array);
    }


    @Test
    public void testReplacingObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("baz", "qux")
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                             Json.createJsonPointer("/baz"),
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("boo", patched.getString("baz"));
        assertEquals("bar", patched.getString("foo"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"boo\"}", toJsonString(patched));
    }

    @Test
    public void testReplacingArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("qux"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                             Json.createJsonPointer("/foo/1"),
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertNotSame(object.getJsonArray("foo"), array);
        assertEquals(2, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("boo", array.getString(1));

        assertEquals("{\"foo\":[\"bar\",\"boo\"]}", toJsonString(patched));
    }

    @Test
    public void testReplacingArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                             Json.createJsonPointer("/0"),
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals(2, patched.size());
        assertEquals("boo", patched.getString(0));
        assertEquals("qux", patched.getString(1));

        assertEquals("[\"boo\",\"qux\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testReplacingObjectMemberNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                             Json.createJsonPointer("/nomatch"),
                                                                             null,
                                                                             new JsonStringImpl("notneeded")));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testReplacingArrayElementIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                             Json.createJsonPointer("/1"),
                                                                             null,
                                                                             new JsonStringImpl("notneeded")));

        patch.apply(array);
    }


    @Test
    public void testMovingObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createObjectBuilder()
                                                .add("bar", "baz")
                                                .add("waldo", "fred"))
                                .add("qux", Json.createObjectBuilder()
                                                .add("corge", "grault"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/qux/thud"),
                                                                             Json.createJsonPointer("/foo/waldo"),
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonObject foo = patched.getJsonObject("foo");
        assertNotNull(foo);
        assertEquals("baz", foo.getString("bar"));
        assertFalse("JsonObject with key 'foo' must not contain 'waldo'", foo.containsKey("waldo"));

        JsonObject qux = patched.getJsonObject("qux");
        assertNotNull(qux);
        assertEquals("grault", qux.getString("corge"));
        assertEquals("fred", qux.getString("thud"));

        assertEquals("{\"foo\":{\"bar\":\"baz\"},\"qux\":{\"corge\":\"grault\",\"thud\":\"fred\"}}", toJsonString(patched));
    }

    @Test
    public void testMovingArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("all")
                                                .add("grass")
                                                .add("cows")
                                                .add("eat"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/foo/3"),
                                                                             Json.createJsonPointer("/foo/1"),
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("all", array.getString(0));
        assertEquals("cows", array.getString(1));
        assertEquals("eat", array.getString(2));
        assertEquals("grass", array.getString(3));

        assertEquals("{\"foo\":[\"all\",\"cows\",\"eat\",\"grass\"]}", toJsonString(patched));
    }

    @Test
    public void testMovingArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("two")
                              .add("three")
                              .add("four")
                              .add("one")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/0"),
                                                                             Json.createJsonPointer("/3"),
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals("one", patched.getString(0));
        assertEquals("two", patched.getString(1));
        assertEquals("three", patched.getString(2));
        assertEquals("four", patched.getString(3));
    }

    @Test
    public void testMovingArrayElementToObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("one")
                                                .add("two")
                                                .add("dog"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/bar"),
                                                                             Json.createJsonPointer("/foo/2"),
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals(2, patched.size());

        JsonArray array = patched.getJsonArray("foo");
        assertEquals(2, array.size());
        assertEquals("one", array.getString(0));
        assertEquals("two", array.getString(1));

        assertEquals("dog", patched.getString("bar"));

        assertEquals("{\"foo\":[\"one\",\"two\"],\"bar\":\"dog\"}", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberNonexistingFrom() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/baz"),
                                                                             Json.createJsonPointer("/nomatch"),
                                                                             null));

        patch.apply(object);

    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/nomatch/child"),
                                                                             Json.createJsonPointer("/foo"),
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberMoveToSubFrom() {

        JsonObject object = Json.createObjectBuilder()
                                .add("object", Json.createObjectBuilder()
                                                   .add("key", "value"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/object/key"),
                                                                             Json.createJsonPointer("/object"),
                                                                             null));

        patch.apply(object);
    }


    @Test
    public void testCopyObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                               .add("foo", "bar")
                               .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/baz"),
                                                                             Json.createJsonPointer("/foo"),
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals(2, patched.size());
        assertEquals("bar", patched.getString("foo"));
        assertEquals("bar", patched.getString("baz"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"bar\"}", toJsonString(patched));
    }

    @Test
    public void testCopyArrayMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                                 Json.createJsonPointer("/foo/-"),
                                                                                 Json.createJsonPointer("/foo/0"),
                                                                                 null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertEquals(3, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));
        assertEquals("bar", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"baz\",\"bar\"]}", toJsonString(patched));
    }

    @Test
    public void testCopyArrayMemberPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .build();


        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/0"),
                                                                             Json.createJsonPointer("/1"),
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals(3, patched.size());
        assertEquals("bar", patched.getString(0));
        assertEquals("foo", patched.getString(1));
        assertEquals("bar", patched.getString(2));

        assertEquals("[\"bar\",\"foo\",\"bar\"]", toJsonString(patched));
    }

    @Test
    public void testCopyObjectMemberToObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("name", "Hugo")
                                .add("partner", Json.createObjectBuilder()
                                                    .add("name", "Leia")
                                                    .add("partner", JsonValue.EMPTY_JSON_OBJECT))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/partner/partner/name"),
                                                                             Json.createJsonPointer("/name"),
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("Hugo", patched.getString("name"));

        JsonObject partner = patched.getJsonObject("partner");
        assertEquals("Leia", partner.getString("name"));

        JsonObject parent = partner.getJsonObject("partner");
        assertEquals(patched.getString("name"), parent.getString("name"));

        assertEquals("{\"name\":\"Hugo\",\"partner\":{\"name\":\"Leia\",\"partner\":{\"name\":\"Hugo\"}}}", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testCopyObjectMemberFromNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/notneeded"),
                                                                             Json.createJsonPointer("/nomatch"),
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testCopyObjectMemberToNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/path/nomatch"),
                                                                             Json.createJsonPointer("/foo"),
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testCopyArrayMemberFromIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/-"),
                                                                             Json.createJsonPointer("/2"),
                                                                             null));

        patch.apply(array);
    }

    @Test(expected = JsonException.class)
    public void testCopyArrayMemberToIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/1"),
                                                                             Json.createJsonPointer("/-"),
                                                                             null));

        patch.apply(array);
    }


    @Test
    public void testTestingObjectMemberValueSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/foo"),
                                                                             null,
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingObjectMemberValueFailed() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/foo"),
                                                                             null,
                                                                             Json.createArrayBuilder().build()));

        patch.apply(object);
    }

    @Test
    public void testTestingArrayAsObjectMemberSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("name", "Thor")
                                .add("parents", Json.createArrayBuilder()
                                                    .add("Odin")
                                                    .add("Forjgyn"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/parents"),
                                                                             null,
                                                                             Json.createArrayBuilder() // yessss, we really want to create a new JsonArray ;)
                                                                                 .add("Odin")
                                                                                 .add("Forjgyn")
                                                                                 .build()));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayAsObjectMemberFailed() {

        JsonObject object = Json.createObjectBuilder()
                                .add("magic", "array")
                                .add("numbers", Json.createArrayBuilder()
                                                    .add(1)
                                                    .add(2))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/numbers"),
                                                                             null,
                                                                             Json.createArrayBuilder() // different ordering
                                                                                 .add(2)
                                                                                 .add(1)
                                                                                 .build()));

        patch.apply(object);
    }

    @Test
    public void testTestingArrayElementSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/foo/1"),
                                                                             null,
                                                                             new JsonStringImpl("baz")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test
    public void testTestingArrayElementPlainArraySuccess() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/2"),
                                                                             null,
                                                                             new JsonStringImpl("qux")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertSame(array, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayElementPlainArrayFailed() {

        JsonArray array = Json.createArrayBuilder()
                              .add(1)
                              .add("2")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/0"),
                                                                             null,
                                                                             new JsonStringImpl("bar")));

        patch.apply(array);
    }

    @Test(expected = JsonException.class)
    public void testTestingObjectMemeberNonexistentTarget() {

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/nomatch"),
                                                                             null,
                                                                             JsonValue.EMPTY_JSON_OBJECT));

        patch.apply(JsonValue.EMPTY_JSON_OBJECT);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayElementIndexOutOfBounds() {

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                             Json.createJsonPointer("/3"),
                                                                             null,
                                                                             JsonValue.EMPTY_JSON_OBJECT));

        patch.apply(JsonValue.EMPTY_JSON_ARRAY);
    }


    @Test
    public void testAddObjectMemberAlreadyExists() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .add("baz", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/foo"),
                                                                             null,
                                                                             new JsonStringImpl("abcd")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("abcd", patched.getString("foo"));
        assertEquals("qux", patched.getString("baz"));

        assertEquals("{\"foo\":\"abcd\",\"baz\":\"qux\"}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementToEmptyArray() {

        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/-"),
                                                                             null,
                                                                             new JsonStringImpl("foo")));

        JsonArray patched = patch.apply(JsonValue.EMPTY_JSON_ARRAY);
        assertNotNull(patched);
        assertEquals(1, patched.size());
        assertEquals("foo", patched.getString(0));
    }

    @Test
    public void testPatchWithMoreOperations() {

        JsonObject object = Json.createObjectBuilder()
                                .add("family", Json.createObjectBuilder()
                                                   .add("children", JsonValue.EMPTY_JSON_ARRAY))
                                .build();

        // i know this can be done with PatchBuilder but
        // currently it's not implemented and its fun ;)
        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/family/father"),
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Gaio Modry Effect")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/family/mother"),
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Cassius vom Hause Clarabella")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                             Json.createJsonPointer("/family/children/0"),
                                                                             Json.createJsonPointer("/family/mother"),
                                                                             null),
                                                new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                             Json.createJsonPointer("/family/mother"),
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Aimee vom Hause Clarabella")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                             Json.createJsonPointer("/pedigree"),
                                                                             Json.createJsonPointer("/family"),
                                                                             null),
                                                new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                             Json.createJsonPointer("/family"),
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonObject pedigree = patched.getJsonObject("pedigree");
        assertEquals("Gaio Modry Effect", pedigree.getJsonObject("father").getString("name"));
        assertEquals("Aimee vom Hause Clarabella", pedigree.getJsonObject("mother").getString("name"));
        assertEquals("Cassius vom Hause Clarabella", pedigree.getJsonArray("children").getJsonObject(0).getString("name"));

        assertEquals("{\"pedigree\":{" +
                     "\"children\":[" +
                     "{\"name\":\"Cassius vom Hause Clarabella\"}]," +
                     "\"mother\":{\"name\":\"Aimee vom Hause Clarabella\"}," +
                     "\"father\":{\"name\":\"Gaio Modry Effect\"}}}", toJsonString(patched));
    }



    private static String toJsonString(JsonStructure value) {
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(value);
        return writer.toString();
    }

}
