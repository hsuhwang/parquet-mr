/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.parquet.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

import static org.apache.parquet.schema.OriginalType.*;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.*;
import static org.apache.parquet.schema.Type.Repetition.*;

public class TestTypeBuilders {
  @Test
  public void testPaperExample() {
    MessageType expected =
        new MessageType("Document",
            new PrimitiveType(REQUIRED, INT64, "DocId"),
            new GroupType(OPTIONAL, "Links",
                new PrimitiveType(REPEATED, INT64, "Backward"),
                new PrimitiveType(REPEATED, INT64, "Forward")),
            new GroupType(REPEATED, "Name",
                new GroupType(REPEATED, "Language",
                    new PrimitiveType(REQUIRED, BINARY, "Code"),
                    new PrimitiveType(REQUIRED, BINARY, "Country")),
                new PrimitiveType(OPTIONAL, BINARY, "Url")));
    MessageType builderType = Types.buildMessage()
        .required(INT64).named("DocId")
        .optionalGroup()
            .repeated(INT64).named("Backward")
            .repeated(INT64).named("Forward")
            .named("Links")
        .repeatedGroup()
            .repeatedGroup()
                .required(BINARY).named("Code")
                .required(BINARY).named("Country")
            .named("Language")
            .optional(BINARY).named("Url")
            .named("Name")
        .named("Document");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testGroupTypeConstruction() {
    PrimitiveType f1 = Types.required(BINARY).as(UTF8).named("f1");
    PrimitiveType f2 = Types.required(INT32).named("f2");
    PrimitiveType f3 = Types.optional(INT32).named("f3");
    String name = "group";
    for (Type.Repetition repetition : Type.Repetition.values()) {
      GroupType expected = new GroupType(repetition, name,
          f1,
          new GroupType(repetition, "g1", f2, f3));
      GroupType built = Types.buildGroup(repetition)
          .addField(f1)
          .group(repetition).addFields(f2, f3).named("g1")
          .named(name);
      Assert.assertEquals(expected, built);

      switch (repetition) {
        case REQUIRED:
          built = Types.requiredGroup()
              .addField(f1)
              .requiredGroup().addFields(f2, f3).named("g1")
              .named(name);
          break;
        case OPTIONAL:
          built = Types.optionalGroup()
              .addField(f1)
              .optionalGroup().addFields(f2, f3).named("g1")
              .named(name);
          break;
        case REPEATED:
          built = Types.repeatedGroup()
              .addField(f1)
              .repeatedGroup().addFields(f2, f3).named("g1")
              .named(name);
          break;
      }
      Assert.assertEquals(expected, built);
    }
  }

  @Test
  public void testPrimitiveTypeConstruction() {
    PrimitiveTypeName[] types = new PrimitiveTypeName[] {
        BOOLEAN, INT32, INT64, INT96, FLOAT, DOUBLE, BINARY
    };
    for (PrimitiveTypeName type : types) {
      String name = type.toString() + "_";
      for (Type.Repetition repetition : Type.Repetition.values()) {
        PrimitiveType expected = new PrimitiveType(repetition, type, name);
        PrimitiveType built = Types.primitive(type, repetition).named(name);
        Assert.assertEquals(expected, built);
        switch (repetition) {
          case REQUIRED:
            built = Types.required(type).named(name);
            break;
          case OPTIONAL:
            built = Types.optional(type).named(name);
            break;
          case REPEATED:
            built = Types.repeated(type).named(name);
            break;
        }
        Assert.assertEquals(expected, built);
      }
    }
  }

  @Test
  public void testFixedTypeConstruction() {
    String name = "fixed_";
    int len = 5;
    for (Type.Repetition repetition : Type.Repetition.values()) {
      PrimitiveType expected = new PrimitiveType(
          repetition, FIXED_LEN_BYTE_ARRAY, len, name);
      PrimitiveType built = Types.primitive(FIXED_LEN_BYTE_ARRAY, repetition)
          .length(len).named(name);
      Assert.assertEquals(expected, built);
      switch (repetition) {
        case REQUIRED:
          built = Types.required(FIXED_LEN_BYTE_ARRAY).length(len).named(name);
          break;
        case OPTIONAL:
          built = Types.optional(FIXED_LEN_BYTE_ARRAY).length(len).named(name);
          break;
        case REPEATED:
          built = Types.repeated(FIXED_LEN_BYTE_ARRAY).length(len).named(name);
          break;
      }
      Assert.assertEquals(expected, built);
    }
  }

  @Test
  public void testEmptyGroup() {
    assertThrows("Should complain that required group is empty",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.requiredGroup().named("g");
          }
        });
    assertThrows("Should complain that optional group is empty",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.optionalGroup().named("g");
          }
        });
    assertThrows("Should complain that repeated group is empty",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.repeatedGroup().named("g");
          }
        });
  }

  @Test
  @Ignore(value="Enforcing this breaks tests in parquet-thrift")
  public void testEmptyMessage() {
    assertThrows("Should complain that message is empty",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage().named("m");
          }
        });
  }

  @Test(expected=IllegalArgumentException.class)
  public void testFixedWithoutLength() {
    Types.required(FIXED_LEN_BYTE_ARRAY).named("fixed");
  }

  @Test
  public void testFixedWithLength() {
    PrimitiveType expected = new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 7, "fixed");
    PrimitiveType fixed = Types.required(FIXED_LEN_BYTE_ARRAY).length(7).named("fixed");
    Assert.assertEquals(expected, fixed);
  }

  @Test
  public void testFixedLengthEquals() {
    Type f4 = Types.required(FIXED_LEN_BYTE_ARRAY).length(4).named("f4");
    Type f8 = Types.required(FIXED_LEN_BYTE_ARRAY).length(8).named("f8");
    Assert.assertFalse("Types with different lengths should not be equal",
        f4.equals(f8));
  }

  @Test
  public void testDecimalAnnotation() {
    // int32 primitive type
    MessageType expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, INT32, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 2), null));
    MessageType builderType = Types.buildMessage()
        .required(INT32)
            .as(DECIMAL).precision(9).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
    // int64 primitive type
    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, INT64, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(18, 2), null));
    builderType = Types.buildMessage()
        .required(INT64)
            .as(DECIMAL).precision(18).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
    // binary primitive type
    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, BINARY, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 2), null));
    builderType = Types.buildMessage()
        .required(BINARY).as(DECIMAL).precision(9).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
    // fixed primitive type
    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 4, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 2), null));
    builderType = Types.buildMessage()
        .required(FIXED_LEN_BYTE_ARRAY).length(4)
            .as(DECIMAL).precision(9).scale(2)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testDecimalAnnotationMissingScale() {
    MessageType expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, INT32, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 0), null));
    MessageType builderType = Types.buildMessage()
        .required(INT32)
            .as(DECIMAL).precision(9)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);

    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, INT64, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 0), null));
    builderType = Types.buildMessage()
        .required(INT64)
            .as(DECIMAL).precision(9)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);

    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, BINARY, 0, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 0), null));
    builderType = Types.buildMessage()
        .required(BINARY).as(DECIMAL).precision(9)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);

    expected = new MessageType("DecimalMessage",
        new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 7, "aDecimal",
            DECIMAL, new DecimalMetadata(9, 0), null));
    builderType = Types.buildMessage()
        .required(FIXED_LEN_BYTE_ARRAY).length(7)
            .as(DECIMAL).precision(9)
            .named("aDecimal")
        .named("DecimalMessage");
    Assert.assertEquals(expected, builderType);
  }

  @Test
  public void testDecimalAnnotationMissingPrecision() {
    assertThrows("Should reject decimal annotation without precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(INT32).as(DECIMAL).scale(2)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject decimal annotation without precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(INT64).as(DECIMAL).scale(2)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject decimal annotation without precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(BINARY).as(DECIMAL).scale(2)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject decimal annotation without precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(FIXED_LEN_BYTE_ARRAY).length(7)
                .as(DECIMAL).scale(2)
                .named("aDecimal")
                .named("DecimalMessage");
          }
        }
    );
  }

  @Test
  public void testDecimalAnnotationPrecisionScaleBound() {
    assertThrows("Should reject scale greater than precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(INT32).as(DECIMAL).precision(3).scale(4)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject scale greater than precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(INT64).as(DECIMAL).precision(3).scale(4)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject scale greater than precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(BINARY).as(DECIMAL).precision(3).scale(4)
                    .named("aDecimal")
                .named("DecimalMessage");
          }
        });
    assertThrows("Should reject scale greater than precision",
        IllegalArgumentException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.buildMessage()
                .required(FIXED_LEN_BYTE_ARRAY).length(7)
                .as(DECIMAL).precision(3).scale(4)
                .named("aDecimal")
                .named("DecimalMessage");
          }
        }
    );
  }

  @Test
  public void testDecimalAnnotationLengthCheck() {
    // maximum precision for 4 bytes is 9
    assertThrows("should reject precision 10 with length 4",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(4)
                .as(DECIMAL).precision(10).scale(2)
                .named("aDecimal");
          }
        });
    assertThrows("should reject precision 10 with length 4",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(INT32)
                .as(DECIMAL).precision(10).scale(2)
                .named("aDecimal");
          }
        });
    // maximum precision for 8 bytes is 19
    assertThrows("should reject precision 19 with length 8",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(8)
                .as(DECIMAL).precision(19).scale(4)
                .named("aDecimal");
          }
        });
    assertThrows("should reject precision 19 with length 8",
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(INT64).length(8)
                .as(DECIMAL).precision(19).scale(4)
                .named("aDecimal");
          }
        }
    );
  }

  @Test
  public void testDECIMALAnnotationRejectsUnsupportedTypes() {
    PrimitiveTypeName[] unsupported = new PrimitiveTypeName[]{
        BOOLEAN, INT96, DOUBLE, FLOAT
    };
    for (final PrimitiveTypeName type : unsupported) {
      assertThrows("Should reject non-binary type: " + type,
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(type)
                  .as(DECIMAL).precision(9).scale(2)
                  .named("d");
            }
          });
    }
  }

  @Test
  public void testBinaryAnnotations() {
    OriginalType[] types = new OriginalType[] {
        UTF8, JSON, BSON};
    for (final OriginalType logicalType : types) {
      PrimitiveType expected = new PrimitiveType(REQUIRED, BINARY, "col", logicalType);
      PrimitiveType string = Types.required(BINARY).as(logicalType).named("col");
      Assert.assertEquals(expected, string);
    }
  }

  @Test
  public void testBinaryAnnotationsRejectsNonBinary() {
    OriginalType[] types = new OriginalType[] {
        UTF8, JSON, BSON};
    for (final OriginalType logicalType : types) {
      PrimitiveTypeName[] nonBinary = new PrimitiveTypeName[]{
          BOOLEAN, INT32, INT64, INT96, DOUBLE, FLOAT
      };
      for (final PrimitiveTypeName type : nonBinary) {
        assertThrows("Should reject non-binary type: " + type,
            IllegalStateException.class, new Callable<Type>() {
              @Override
              public Type call() throws Exception {
                return Types.required(type).as(logicalType).named("col");
              }
            });
      }
      assertThrows("Should reject non-binary type: FIXED_LEN_BYTE_ARRAY",
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(FIXED_LEN_BYTE_ARRAY).length(1)
                  .as(logicalType).named("col");
            }
          });
    }
  }

  @Test
  public void testInt32Annotations() {
    OriginalType[] types = new OriginalType[] {
        DATE, TIME_MILLIS, UINT_8, UINT_16, UINT_32, INT_8, INT_16, INT_32};
    for (OriginalType logicalType : types) {
      PrimitiveType expected = new PrimitiveType(REQUIRED, INT32, "col", logicalType);
      PrimitiveType date = Types.required(INT32).as(logicalType).named("col");
      Assert.assertEquals(expected, date);
    }
  }

  @Test
  public void testInt32AnnotationsRejectNonInt32() {
    OriginalType[] types = new OriginalType[] {
        DATE, TIME_MILLIS, UINT_8, UINT_16, UINT_32, INT_8, INT_16, INT_32};
    for (final OriginalType logicalType : types) {
      PrimitiveTypeName[] nonInt32 = new PrimitiveTypeName[]{
          BOOLEAN, INT64, INT96, DOUBLE, FLOAT, BINARY
      };
      for (final PrimitiveTypeName type : nonInt32) {
        assertThrows("Should reject non-int32 type: " + type,
            IllegalStateException.class, new Callable<Type>() {
              @Override
              public Type call() throws Exception {
                return Types.required(type).as(logicalType).named("col");
              }
            });
      }
      assertThrows("Should reject non-int32 type: FIXED_LEN_BYTE_ARRAY",
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(FIXED_LEN_BYTE_ARRAY).length(1)
                  .as(logicalType).named("col");
            }
          });
    }
  }

  @Test
  public void testInt64Annotations() {
    OriginalType[] types = new OriginalType[] {
        TIMESTAMP_MILLIS, UINT_64, INT_64};
    for (OriginalType logicalType : types) {
      PrimitiveType expected = new PrimitiveType(REQUIRED, INT64, "col", logicalType);
      PrimitiveType date = Types.required(INT64).as(logicalType).named("col");
      Assert.assertEquals(expected, date);
    }
  }

  @Test
  public void testInt64AnnotationsRejectNonInt64() {
    OriginalType[] types = new OriginalType[] {
        TIMESTAMP_MILLIS, UINT_64, INT_64};
    for (final OriginalType logicalType : types) {
      PrimitiveTypeName[] nonInt64 = new PrimitiveTypeName[]{
          BOOLEAN, INT32, INT96, DOUBLE, FLOAT, BINARY
      };
      for (final PrimitiveTypeName type : nonInt64) {
        assertThrows("Should reject non-int64 type: " + type,
            IllegalStateException.class, new Callable<Type>() {
              @Override
              public Type call() throws Exception {
                return Types.required(type).as(logicalType).named("col");
              }
            });
      }
      assertThrows("Should reject non-int64 type: FIXED_LEN_BYTE_ARRAY",
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(FIXED_LEN_BYTE_ARRAY).length(1)
                  .as(logicalType).named("col");
            }
          });
    }
  }

  @Test
  public void testIntervalAnnotation() {
    PrimitiveType expected = new PrimitiveType(REQUIRED, FIXED_LEN_BYTE_ARRAY, 12, "interval", INTERVAL);
    PrimitiveType string = Types.required(FIXED_LEN_BYTE_ARRAY).length(12)
        .as(INTERVAL).named("interval");
    Assert.assertEquals(expected, string);
  }

  @Test
  public void testIntervalAnnotationRejectsNonFixed() {
    PrimitiveTypeName[] nonFixed = new PrimitiveTypeName[]{
        BOOLEAN, INT32, INT64, INT96, DOUBLE, FLOAT, BINARY
    };
    for (final PrimitiveTypeName type : nonFixed) {
      assertThrows("Should reject non-fixed type: " + type,
          IllegalStateException.class, new Callable<Type>() {
            @Override
            public Type call() throws Exception {
              return Types.required(type).as(INTERVAL).named("interval");
            }
          });
    }
  }

  @Test
  public void testIntervalAnnotationRejectsNonFixed12() {
    assertThrows("Should reject fixed with length != 12: " + 11,
        IllegalStateException.class, new Callable<Type>() {
          @Override
          public Type call() throws Exception {
            return Types.required(FIXED_LEN_BYTE_ARRAY).length(11)
                .as(INTERVAL).named("interval");
          }
        });
  }

  @Test
  public void testRequiredMap() {
    List<Type> typeList = new ArrayList<Type>();
    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new PrimitiveType(REQUIRED, INT64, "value"));
    GroupType expected = new GroupType(REQUIRED, "myMap", OriginalType.MAP, new GroupType(REPEATED,
        "map",
        typeList));
    GroupType actual = Types.requiredMap()
        .key(INT64)
        .requiredValue(INT64)
        .named("myMap");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalMap() {
    List<Type> typeList = new ArrayList<Type>();
    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new PrimitiveType(REQUIRED, INT64, "value"));
    GroupType expected = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED,
        "map",
        typeList));
    GroupType actual = Types.optionalMap()
        .key(INT64)
        .requiredValue(INT64)
        .named("myMap");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithRequiredValue() {
    List<Type> typeList = new ArrayList<Type>();
    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new PrimitiveType(REQUIRED, INT64, "value"));
    GroupType map = new GroupType(REQUIRED, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));
    MessageType expected = new MessageType("mapParent", map);
    GroupType actual = Types.buildMessage().requiredMap()
        .key(INT64)
        .requiredValue(INT64)
        .named("myMap").named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithOptionalValue() {
    List<Type> typeList = new ArrayList<Type>();
    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new PrimitiveType(OPTIONAL, INT64, "value"));
    GroupType map = new GroupType(REQUIRED, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));
    MessageType expected = new MessageType("mapParent", map);
    GroupType actual = Types.buildMessage().requiredMap()
        .key(INT64)
        .optionalValue(INT64)
        .named("myMap").named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndOptionalGroupValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "second"));
    typeList.add(new GroupType(REQUIRED, "key", keyFields));

    List<Type> valueFields = new ArrayList<Type>();
    valueFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "one"));
    valueFields.add(new PrimitiveType(OPTIONAL, INT32, "two"));
    typeList.add(new GroupType(OPTIONAL, "value", valueFields));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    GroupType actual = Types.optionalMap()
        .groupKey()
          .optional(INT64).named("first")
          .optional(DOUBLE).named("second")
        .optionalGroupValue()
          .optional(DOUBLE).named("one")
          .optional(INT32).named("two")
        .named("myMap");
    Assert.assertEquals(map, actual);
  }

  @Test
  public void testMapWithGroupKeyAndRequiredGroupValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "second"));
    typeList.add(new GroupType(REQUIRED, "key", keyFields));

    List<Type> valueFields = new ArrayList<Type>();
    valueFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "one"));
    valueFields.add(new PrimitiveType(OPTIONAL, INT32, "two"));
    typeList.add(new GroupType(REQUIRED, "value", valueFields));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .optionalMap()
              .groupKey()
                .optional(INT64).named("first")
                .optional(DOUBLE).named("second")
              .requiredGroupValue()
                .optional(DOUBLE).named("one")
                .optional(INT32).named("two")
              .named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndOptionalValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "second"));
    typeList.add(new GroupType(REQUIRED, "key", keyFields));

    typeList.add(new PrimitiveType(OPTIONAL, DOUBLE, "value"));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .optionalMap()
              .groupKey()
                .optional(INT64).named("first")
                .optional(DOUBLE).named("second")
              .optionalValue(DOUBLE).named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndRequiredValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "second"));
    typeList.add(new GroupType(REQUIRED, "key", keyFields));

    typeList.add(new PrimitiveType(REQUIRED, DOUBLE, "value"));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .optionalMap()
              .groupKey()
                .optional(INT64).named("first")
                .optional(DOUBLE).named("second")
              .requiredValue(DOUBLE).named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithOptionalGroupValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "second"));
    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));

    List<Type> valueFields = new ArrayList<Type>();
    valueFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "one"));
    valueFields.add(new PrimitiveType(OPTIONAL, INT32, "two"));
    typeList.add(new GroupType(OPTIONAL, "value", valueFields));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .optionalMap()
              .key(INT64)
              .optionalGroupValue()
                .optional(DOUBLE).named("one")
                .optional(INT32).named("two")
              .named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithRequiredGroupValue() {
    List<Type> typeList = new ArrayList<Type>();

    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));

    List<Type> valueFields = new ArrayList<Type>();
    valueFields.add(new PrimitiveType(OPTIONAL, DOUBLE, "one"));
    valueFields.add(new PrimitiveType(OPTIONAL, INT32, "two"));
    typeList.add(new GroupType(REQUIRED, "value", valueFields));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .optionalMap()
              .key(INT64)
              .requiredGroupValue()
                .optional(DOUBLE).named("one")
                .optional(INT32).named("two")
              .named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithNestedGroupKeyAndNestedGroupValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> innerFields = new ArrayList<Type>();
    innerFields.add(new PrimitiveType(REQUIRED, FLOAT, "inner_key_1"));
    innerFields.add(new PrimitiveType(OPTIONAL, INT32, "inner_key_2"));

    List<Type> keyFields = new ArrayList<Type>();
    keyFields.add(new PrimitiveType(OPTIONAL, INT64, "first"));
    keyFields.add(new GroupType(REQUIRED, "second", innerFields));
    typeList.add(new GroupType(REQUIRED, "key", keyFields));

    List<Type> valueFields = new ArrayList<Type>();
    valueFields.add(new GroupType(OPTIONAL, "one", innerFields));
    valueFields.add(new PrimitiveType(OPTIONAL, INT32, "two"));
    typeList.add(new GroupType(OPTIONAL, "value", valueFields));

    GroupType map = new GroupType(REQUIRED, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
            .requiredMap()
              .groupKey()
                .optional(INT64).named("first")
                .requiredGroup()
                  .required(FLOAT).named("inner_key_1")
                  .optional(INT32).named("inner_key_2")
                .named("second")
              .optionalGroupValue()
                .optionalGroup()
                  .required(FLOAT).named("inner_key_1")
                  .optional(INT32).named("inner_key_2")
                .named("one")
                .optional(INT32).named("two")
              .named("myMap")
            .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

    @Test
    public void testMapWithRequiredListValue() {
        List<Type> typeList = new ArrayList<Type>();

        typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
        typeList.add(new GroupType(REQUIRED, "value", OriginalType.LIST,
                new GroupType(REPEATED,
                        "list",
                        new PrimitiveType(OPTIONAL, INT64, "element"))));

        GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
                typeList));

        MessageType expected = new MessageType("mapParent", map);
        GroupType actual =
                Types.buildMessage()
                  .optionalMap()
                    .key(INT64)
                    .requiredListValue()
                      .optionalElement(INT64)
                  .named("myMap")
                .named("mapParent");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapWithOptionalListValue() {
        List<Type> typeList = new ArrayList<Type>();

        typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
        typeList.add(new GroupType(OPTIONAL, "value", OriginalType.LIST,
                new GroupType(REPEATED,
                        "list",
                        new PrimitiveType(OPTIONAL, INT64, "element"))));

        GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
                typeList));

        MessageType expected = new MessageType("mapParent", map);
        GroupType actual =
                Types.buildMessage()
                  .optionalMap()
                    .key(INT64)
                    .optionalListValue()
                      .optionalElement(INT64)
                  .named("myMap")
                .named("mapParent");
        Assert.assertEquals(expected, actual);
    }

  @Test
  public void testMapWithRequiredMapValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> innerMapTypeList = new ArrayList<Type>();
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "value"));

    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new GroupType(REQUIRED, "value", OriginalType.MAP,
        new GroupType(REPEATED, "map", innerMapTypeList)));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
          .optionalMap()
            .key(INT64)
            .requiredMapValue()
              .key(INT64)
              .requiredValue(INT64)
          .named("myMap")
        .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithOptionalMapValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> innerMapTypeList = new ArrayList<Type>();
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "value"));

    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    typeList.add(new GroupType(OPTIONAL, "value", OriginalType.MAP,
        new GroupType(REPEATED, "map", innerMapTypeList)));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .key(INT64)
          .optionalMapValue()
            .key(INT64)
            .requiredValue(INT64)
      .named("myMap")
    .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndRequiredListValue() {
    List<Type> typeList = new ArrayList<Type>();

    typeList.add(new GroupType(REQUIRED, "key", new PrimitiveType(REQUIRED, INT64,
        "first"
        )));
    typeList.add(new GroupType(REQUIRED, "value", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element"))));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .groupKey()
            .required(INT64)
            .named("first")
          .requiredListValue()
            .optionalElement(INT64)
      .named("myMap")
    .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndOptionalListValue() {
    List<Type> typeList = new ArrayList<Type>();

    typeList.add(new GroupType(REQUIRED, "key", new PrimitiveType(REQUIRED, INT64,
        "first"
    )));
    typeList.add(new GroupType(OPTIONAL, "value", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element"))));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .groupKey()
            .required(INT64)
          .named("first")
          .optionalListValue()
            .optionalElement(INT64)
        .named("myMap")
      .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndRequiredMapValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> innerMapTypeList = new ArrayList<Type>();
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "value"));


    typeList.add(new GroupType(REQUIRED, "key", new PrimitiveType(REQUIRED, INT64,
        "first"
    )));
    typeList.add(new GroupType(REQUIRED, "value", OriginalType.MAP,
        new GroupType(REPEATED, "map", innerMapTypeList)));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .groupKey()
            .required(INT64)
          .named("first")
          .requiredMapValue()
            .key(INT64)
            .requiredValue(INT64)
        .named("myMap")
      .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithGroupKeyAndOptionalMapValue() {
    List<Type> typeList = new ArrayList<Type>();

    List<Type> innerMapTypeList = new ArrayList<Type>();
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    innerMapTypeList.add(new PrimitiveType(REQUIRED, INT64, "value"));


    typeList.add(new GroupType(REQUIRED, "key", new PrimitiveType(REQUIRED, INT64,
        "first"
    )));
    typeList.add(new GroupType(OPTIONAL, "value", OriginalType.MAP,
        new GroupType(REPEATED, "map", innerMapTypeList)));

    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .groupKey()
            .required(INT64)
          .named("first")
          .optionalMapValue()
            .key(INT64)
            .requiredValue(INT64)
        .named("myMap")
      .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithNullValue() {
    List<Type> typeList = new ArrayList<Type>();

    typeList.add(new PrimitiveType(REQUIRED, INT64, "key"));
    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
      Types.buildMessage()
        .optionalMap()
          .key(INT64)
        .named("myMap")
      .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithDefaultKeyAndNullValue() {
    List<Type> typeList = new ArrayList<Type>();

    typeList.add(new PrimitiveType(REQUIRED, BINARY, "key", OriginalType.UTF8));
    GroupType map = new GroupType(OPTIONAL, "myMap", OriginalType.MAP, new GroupType(REPEATED, "map",
        typeList));

    MessageType expected = new MessageType("mapParent", map);
    GroupType actual =
        Types.buildMessage()
          .optionalMap()
          .named("myMap")
        .named("mapParent");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMapWithPreBuiltKeyAndValueTypes() {
    Type keyType = Types.required(INT64).named("key");
    Type valueType = Types.required(BOOLEAN).named("value");

    GroupType map = new GroupType(REQUIRED, "myMap", OriginalType.MAP,
        new GroupType(REPEATED, "map", new Type[] {
            keyType,
            valueType
        }));
    MessageType expected = new MessageType("mapParent", map);

    GroupType actual = Types.buildMessage()
        .requiredMap()
          .key(keyType)
          .value(valueType)
        .named("myMap")
      .named("mapParent");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testListWithRequiredPreBuiltElement() {
    GroupType expected = new GroupType(REQUIRED, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(REQUIRED, INT64, "element")));
    Type element = Types.primitive(INT64, REQUIRED).named("element");
    Type actual = Types.requiredList()
          .element(element)
        .named("myList");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRequiredList() {
    GroupType expected = new GroupType(REQUIRED, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element")));
    Type actual = Types.requiredList()
        .optionalElement(INT64)
        .named("myList");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalList() {
    GroupType expected = new GroupType(OPTIONAL, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element")));
    Type actual = Types.optionalList()
        .optionalElement(INT64)
        .named("myList");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testListOfReqGroup() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new PrimitiveType(OPTIONAL, BOOLEAN, "field"));
    GroupType expected = new GroupType(REQUIRED, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new GroupType(REQUIRED, "element", fields)));
    Type actual = Types.requiredList()
        .requiredGroupElement()
          .optional(BOOLEAN)
          .named("field")
        .named("myList");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testListOfOptionalGroup() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new PrimitiveType(OPTIONAL, BOOLEAN, "field"));
    GroupType expected = new GroupType(REQUIRED, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new GroupType(OPTIONAL, "element", fields)));
    Type actual = Types.requiredList()
          .optionalGroupElement()
            .optional(BOOLEAN)
            .named("field")
        .named("myList");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRequiredNestedList() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(REQUIRED, "element", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, DOUBLE, "element"))));
    GroupType expected = new GroupType(OPTIONAL, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            fields));

    Type actual =
        Types.optionalList()
          .requiredListElement()
            .optionalElement(DOUBLE)
        .named("myList");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalNestedList() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(OPTIONAL, "element", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, DOUBLE, "element"))));
    GroupType expected = new GroupType(OPTIONAL, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            fields));

    Type actual =
        Types.optionalList()
        .optionalListElement()
          .optionalElement(DOUBLE)
        .named("myList");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRequiredListWithinGroup() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(REQUIRED, "element", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element"))));
    GroupType expected = new GroupType(REQUIRED, "topGroup", fields);
    Type actual = Types.requiredGroup()
          .requiredList()
            .optionalElement(INT64).named("element")
        .named("topGroup");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalListWithinGroup() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(OPTIONAL, "element", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(OPTIONAL, INT64, "element"))));
    GroupType expected = new GroupType(REQUIRED, "topGroup", fields);
    Type actual = Types.requiredGroup()
        .optionalList()
        .optionalElement(INT64).named("element")
        .named("topGroup");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalListWithinGroupWithReqElement() {
    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(OPTIONAL, "element", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            new PrimitiveType(REQUIRED, INT64, "element"))));
    GroupType expected = new GroupType(REQUIRED, "topGroup", fields);
    Type actual =
        Types.requiredGroup()
          .optionalList()
            .requiredElement(INT64).named("element")
          .named("topGroup");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRequiredMapWithinList() {
    List<Type> innerFields = new ArrayList<Type>();
    innerFields.add(new PrimitiveType(REQUIRED, DOUBLE, "key"));
    innerFields.add(new PrimitiveType(REQUIRED, INT32, "value"));

    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(REQUIRED, "element", OriginalType.MAP,
        new GroupType(REPEATED,
            "map",
            innerFields)));
    GroupType expected = new GroupType(OPTIONAL, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            fields));

    Type actual =
        Types.optionalList()
          .requiredMapElement()
            .key(DOUBLE)
            .requiredValue(INT32)
        .named("myList");

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testOptionalMapWithinList() {
    List<Type> innerFields = new ArrayList<Type>();
    innerFields.add(new PrimitiveType(REQUIRED, DOUBLE, "key"));
    innerFields.add(new PrimitiveType(REQUIRED, INT32, "value"));

    List<Type> fields = new ArrayList<Type>();
    fields.add(new GroupType(OPTIONAL, "element", OriginalType.MAP,
        new GroupType(REPEATED,
            "map",
            innerFields)));
    GroupType expected = new GroupType(OPTIONAL, "myList", OriginalType.LIST,
        new GroupType(REPEATED,
            "list",
            fields));

    Type actual =
        Types.optionalList()
          .optionalMapElement()
            .key(DOUBLE)
            .requiredValue(INT32)
        .named("myList");

    Assert.assertEquals(expected, actual);
  }


  /**
   * A convenience method to avoid a large number of @Test(expected=...) tests
   * @param message A String message to describe this assertion
   * @param expected An Exception class that the Runnable should throw
   * @param callable A Callable that is expected to throw the exception
   */
  public static void assertThrows(
      String message, Class<? extends Exception> expected, Callable callable) {
    try {
      callable.call();
      Assert.fail("No exception was thrown (" + message + "), expected: " +
          expected.getName());
    } catch (Exception actual) {
      Assert.assertEquals(message, expected, actual.getClass());
    }
  }
}
