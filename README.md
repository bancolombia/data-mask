# Data Masking Utility

Utility library to use with Jackson-Databind to provide custom 
POJO/JSON serialization and deserialization aiming to protect
sensitive data via masking and/or encrypting-decrypting. 

Functionality:

- Masking an Object string members : Serializing to a pattern string. Eg: masking a credit card number 
  from `"1111 2222 3333 4444"` to `"***************4444"`.


- Encrypting: Converting string input value in a pair of masked an ecrypted values.

  - As an composite String:
    `"1111 2222 3333 4444"` to `"masked_pair=***************4444|<credit card encrypted value>"`
  
  - As a Json Object. Eg: Converting `"1111 2222 3333 4444"` into:
    ```json
    {
      "masked": "***************4444",
      "enc": "<credit card encrypted value>"
    }
    ```

- Decrypting: Converting an encrypted string/json input value to its plain value.

    - From a composite String:
      Restoring `"masked_pair=***************4444|<credit card encrypted value>"` to `"1111 2222 3333 4444"`.

    - From a Json Object. Converting:
      ```json
      {
        "masked": "***************4444",
        "enc": "<credit card encrypted value>"
      }
      ```
      
      back to `"1111 2222 3333 4444"` again.
  
## Installing

With Gradle
```gradle
implementation 'com.github.bancolombia:data-mask:1.0.0-SNAPSHOT'
```

With maven
```maven
<dependency>
  <groupId>com.github.bancolombia</groupId>
  <artifactId>data-mask</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

This library depends on:

- `org.apache.commons:commons-lang3`
- `com.fasterxml.jackson.core:jackson-databind`

## Using

### A. Decorate POJO 

Members to be masked/encrypted should be annotated with `@Mask`, eg: 

```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4)
        private String email;

        @Mask(rightVisible=4, queryOnly=false, format=DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String creditCardNumber;
    }
````

__Anotation Properties__

| Attribute  | Default value  | Description  |
|---|---|---|
| leftVisible  | 0  | Masking: how many characters should remain visible on left. Example: Hello******  |
| rightVisible  | 4  | Masking: how many characters should remain visible on right Example: *****World |
| queryOnly  | true  | `true`: Serialization should generate masked value only. |
|  |  |  `false`: serialization should generate masked value and encrypted value. |
| format | ENCRYPTION_AS_OBJECT | Describes how masked and encrypted data should be serialized. Using `ENCRYPTION_INLINE` means masked and encrypted values together are serialized as string: ```masked_pair=<masked_value>I<encrypted_value>``` |
|  |  | Using `ENCRYPTION_AS_OBJECT`, means masked and encrypted values are serialized as a json object. |


### B. Implement Cipher and Decipher interfaces

This library defines two interfaces: `DataCipher` and `DataDecipher` which are used in the encryption/decryption 
processes.

User of this library must define implementation for both interfaces.


### C. Define and use Custom ObjectMapper

This library defines a custom `ObjectMapper` in order to provide the masking and unmasking functionality, and takes 
as constructor arguments, the implementations of both `DataCipher` and `DataDecipher` interfaces.

```java
var dummyCipher = new DataCipher() {
    @Override
    public String cipher(String plainData) {
        return "the encrypted value";
    }
};

var dummyDecipher = new DataDecipher() {
    @Override
    public String decipher(String encryptedData) {
        return "the plain value";
    }
};

ObjectMapper mapper = new MaskingObjectMapper(dummyCipher, dummyDecipher);
```

So, having this example of annotated class:

```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Customer {
        private String name;

        @Mask(leftVisible = 3, rightVisible=4)
        private String email;

        @Mask(rightVisible=4, queryOnly=false, format=DataMaskingConstants.ENCRYPTION_AS_OBJECT)
        private String creditCardNumber;
    }
````

__Serializing Process__

An instance of example `Customer` annotated class:

```java
    Customer customer = new Customer("Jhon Doe", 
        "jhon.doe123@someservice.com", 
        "4444555566665678");
```

Should be serialized as JSON like this:

```java
String json = mapper.writeValueAsString(customer);
```

```json
{
  "name": "Jhon Doe",
  "email": "Jho**************.com",
  "creditCardNumber": {
    "masked": "************5678",
    "enc": "dGhpcyBzaG91bGQgYmUgYW4gZWNyeXB0ZWQgdmFsdWUK"
  }
}
```

__Deserializing Process__

The deserialization process should construct an instance of the example `Customer` 
with is `creditCardNumber`property in plain text.

```java
String json = "{\n" +
    "\"name\": \"Jhon Doe\",\n" +
    "\"email\": \"Jho**************.com\",\n" +
    "\"creditCardNumber\": {\n" +
    "\"masked\": \"************5678\",\n" +
    "\"enc\": \"dGhpcyBzaG91bGQgYmUgYW4gZWNyeXB0ZWQgdmFsdWUK\"\n" +
    "}\n" +
    "}";

Customer customer = mapper.readValue(json, Customer.class);
assertEquals("4444555566665678", customer.creditCardNumber());
```

# Contribute
TODO: Explain how other users and developers can contribute to make your code better. 
