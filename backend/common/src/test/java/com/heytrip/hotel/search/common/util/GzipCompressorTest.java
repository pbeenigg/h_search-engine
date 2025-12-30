package com.heytrip.hotel.search.common.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GZIP 压缩/解压工具测试
 */
class GzipCompressorTest {

    @Test
    void testCompressAndDecompress() {
        String largeJson = """
                {\\"propertyId\\":10652726,\\"summary\\":{\\"propertyName\\":{\\"englishName\\":\\"Summer Night\\",\\"localName\\":\\"Summer Night\\",\\"displayName\\":\\"Summer Night\\"},\\"address\\":{\\"address1\\":\\"No. 33-3, Xining Street, West Central District\\",\\"address2\\":\\"\\",\\"areaName\\":\\"Tainan City\\",\\"cityName\\":\\"Tainan\\",\\"regionName\\":\\"Asia\\",\\"stateName\\":\\"Tainan City\\",\\"stateId\\":3767,\\"countryName\\":\\"Taiwan\\",\\"postalCode\\":\\"700\\"},\\"gmtOffset\\":8,\\"cityId\\":18347,\\"cityName\\":\\"Tainan\\",\\"countryNameEn\\":\\"Taiwan\\",\\"countryId\\":140,\\"countryCode\\":\\"tw\\",\\"starRating\\":{\\"rating\\":2.5,\\"type\\":1,\\"text\\":\\"These circles are quality ratings awarded by Agoda to Homes and Apartment-like properties based on factors including facilities, size, location, and service. Five circles is the highest rating.\\"},\\"rating\\":{\\"score\\":2.5,\\"type\\":2},\\"coordinate\\":{\\"lat\\":22.997289657592773,\\"lng\\":120.19168853759766},\\"accommodationType\\":{\\"id\\":108,\\"englishName\\":\\"Homestay\\",\\"localName\\":\\"Homestay\\"},\\"propertyType\\":1,\\"hasHostExperience\\":true,\\"blockedNationalities\\":[],\\"propertyUrl\\":\\"/zh-cn/deface-victory-suites-kuala-lumpur/hotel/kuala-lumpur-my.html\\",\\"awardsAndAccolades\\":{\\"advanceGuaranteeProgram\\":{\\"isEligible\\":false}},\\"localLanguage\\":{\\"languageId\\":20,\\"name\\":\\"Chinese (Taiwan)\\",\\"locale\\":\\"zh-tw\\",\\"isSuggested\\":false,\\"shouldShowAmountBeforeCurrency\\":false},\\"sharingUrl\\":null,\\"topSellingPoints\\":[{\\"id\\":8,\\"text\\":\\"Best seller\\",\\"value\\":0.0}],\\"supplierHotelId\\":\\"5746351\\",\\"renovation\\":null,\\"isEasyCancel\\":false,\\"isExcellentCleanliness\\":false,\\"nhaSummary\\":{\\"hostType\\":2,\\"arrivalGuide\\":{\\"checkinMethod\\":null,\\"selfCheckin\\":false,\\"managedCheckin\\":false}},\\"isInclusivePricePolicy\\":false},\\"images\\":[{\\"id\\":\\"154916166\\",\\"type\\":6,\\"caption\\":\\"Exterior view\\",\\"captionId\\":13,\\"category\\":\\"Property views\\",\\"categoryId\\":\\"property\\",\\"urls\\":{\\"normal\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max500/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\",\\"retina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/840x460/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\",\\"superRetina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max1024x768/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\"},\\"thumbnailUrls\\":{\\"normal\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\",\\"retina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\",\\"superRetina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/228753535.jpg?k=10bfe5e4eb215bf83c8b83a9b396980a79bc5ca568732db573480cd9c7e9d827&o=\\"},\\"snippet\\":null},{\\"id\\":\\"665\\",\\"type\\":8,\\"caption\\":\\"\\",\\"captionId\\":0,\\"category\\":\\"Other\\",\\"categoryId\\":\\"other\\",\\"urls\\":{\\"normal\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=414x232\\",\\"retina\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=828x464\\",\\"superRetina\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=1656x928\\"},\\"thumbnailUrls\\":{\\"normal\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=55x55\\",\\"retina\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=110x110\\",\\"superRetina\\":\\"https://pix8.ag-static.cn/city/18347/18347-16x9.jpg?s=220x220\\"},\\"snippet\\":null},{\\"id\\":\\"158285985\\",\\"type\\":7,\\"caption\\":\\"Standard Double Room\\",\\"captionId\\":12,\\"category\\":\\"Rooms\\",\\"categoryId\\":\\"room\\",\\"urls\\":{\\"normal\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max500/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\",\\"retina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/840x460/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\",\\"superRetina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max1024x768/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\"},\\"thumbnailUrls\\":{\\"normal\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\",\\"retina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\",\\"superRetina\\":\\"https://q-xx.bstatic.com/xdata/images/hotel/max300/229689678.jpg?k=1f76093bf91d644982d7a2cdca4035be064e16c95f4ac9d5e17b978803b33e94&o=\\"},\\"snippet\\":null},{\\"id\\":\\"936540428\\",\\"type\\":5,\\"caption\\":\\"\\",\\"captionId\\":13,\\"category\\":\\"Property views\\",\\"categoryId\\":\\"property\\",\\"urls\\":{\\"normal\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=414x232&ar=16x9\\",\\"retina\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=828x464&ar=16x9\\",\\"superRetina\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=1656x928&ar=16x9\\"},\\"thumbnailUrls\\":{\\"normal\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=55x55&ar=16x9\\",\\"retina\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=110x110&ar=16x9\\",\\"superRetina\\":\\"https://pix8.ag-static.cn/property/69645210/0/ada8fb39ff52a0b3d189769abcb4c5dc.jpeg?ce=2&s=220x220&ar=16x9\\"},\\"snippet\\":null},{\\"id\\":\\"154916164\\"
                """;
        
        // 压缩
        byte[] compressed = GzipCompressor.compressString(largeJson);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        assertTrue(compressed.length < largeJson.getBytes().length); // 压缩后应该更小
        
        // 解压
        String decompressed = GzipCompressor.decompressToString(compressed);
        assertEquals(largeJson, decompressed);
    }

    @Test
    void testCompressLargeJson() {
        // 模拟酒店详情 JSON（较大的文本）
        String largeJson = """
                {
                    "Result": {
                        "Detail": {
                            "HotelId": 123456,
                            "HotelName": "测试酒店",
                            "HotelNameEn": "Test Hotel",
                            "Address": "测试地址 123 号",
                            "AddressEn": "123 Test Street",
                            "Description": "这是一段很长的酒店描述文本，包含了酒店的各种信息和设施介绍...",
                            "Facilities": ["WiFi", "停车场", "游泳池", "健身房", "餐厅"],
                            "Images": [
                                "https://example.com/image1.jpg",
                                "https://example.com/image2.jpg",
                                "https://example.com/image3.jpg"
                            ]
                        }
                    }
                }
                """;
        
        byte[] compressed = GzipCompressor.compressString(largeJson);
        String decompressed = GzipCompressor.decompressToString(compressed);
        
        assertEquals(largeJson, decompressed);
        
        // 验证压缩率
        double compressionRatio = (double) compressed.length / largeJson.getBytes().length;
        System.out.println("原始大小: " + largeJson.getBytes().length + " 字节");
        System.out.println("压缩后大小: " + compressed.length + " 字节");
        System.out.println("压缩率: " + String.format("%.2f%%", compressionRatio * 100));
        
        assertTrue(compressionRatio < 1.0); // 应该有压缩效果
    }

    @Test
    void testBase64Encoding() {
        // 模拟完整的存储流程：压缩 -> Base64 编码 -> 存储 -> Base64 解码 -> 解压
        String original = "测试酒店详情数据 Test Hotel Details";
        
        // 压缩并 Base64 编码（模拟存储）
        byte[] compressed = GzipCompressor.compressString(original);
        String base64Encoded = Base64.getEncoder().encodeToString(compressed);
        
        assertNotNull(base64Encoded);
        assertTrue(base64Encoded.length() > 0);
        
        // Base64 解码并解压（模拟读取）
        byte[] decoded = Base64.getDecoder().decode(base64Encoded);
        String decompressed = GzipCompressor.decompressToString(decoded);
        
        assertEquals(original, decompressed);
    }

    @Test
    void testEmptyString() {
        String empty = "";
        byte[] compressed = GzipCompressor.compressString(empty);
        String decompressed = GzipCompressor.decompressToString(compressed);
        assertEquals(empty, decompressed);
    }

    @Test
    void testNullString() {
        byte[] compressed = GzipCompressor.compressString(null);
        assertNotNull(compressed);
        assertEquals(0, compressed.length);
        
        String decompressed = GzipCompressor.decompressToString(null);
        assertEquals("", decompressed);
    }

    @Test
    void testChineseCharacters() {
        String chinese = "这是一段包含中文字符的测试文本，用于验证 UTF-8 编码是否正确处理。";
        
        byte[] compressed = GzipCompressor.compressString(chinese);
        String decompressed = GzipCompressor.decompressToString(compressed);
        
        assertEquals(chinese, decompressed);
    }

    @Test
    void testSpecialCharacters() {
        String special = "特殊字符测试: !@#$%^&*()_+-=[]{}|;':\",./<>?\\n\\t\\r";
        
        byte[] compressed = GzipCompressor.compressString(special);
        String decompressed = GzipCompressor.decompressToString(compressed);
        
        assertEquals(special, decompressed);
    }

    @Test
    void testCompressionRatio() {
        // 测试重复文本的压缩效果
        String repeated = "重复文本".repeat(100);
        
        byte[] compressed = GzipCompressor.compressString(repeated);
        double ratio = (double) compressed.length / repeated.getBytes().length;
        
        System.out.println("重复文本压缩率: " + String.format("%.2f%%", ratio * 100));
        
        // 重复文本应该有很好的压缩效果
        assertTrue(ratio < 0.1); // 压缩率应该小于 10%
    }
}
