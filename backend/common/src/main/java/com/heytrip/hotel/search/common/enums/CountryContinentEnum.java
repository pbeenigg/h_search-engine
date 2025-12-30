package com.heytrip.hotel.search.common.enums;

import lombok.Getter;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 国家-洲 枚举（支持别名匹配与模糊识别）
 */
@Getter
public enum CountryContinentEnum {
    // 亚洲国家
    CN("中国", "China", "CN", ContinentEnum.ASIA,"china","chinese"),
    HK("中国香港特区", "Hong Kong", "HK", ContinentEnum.ASIA,"HongKong","Hong Kong SAR, China","Hong Kong SAR"),
    MO("中国澳门特区", "Macau", "MO", ContinentEnum.ASIA, "Macao","Macau SAR, China","Macau SAR"),
    TW("中国台湾省", "Taiwan", "TW", ContinentEnum.ASIA, "Taiwan Province", "Taiwan","Taiwan, China", "Taiwan (R.O.C.)","R.O.C."),
    JP("日本", "Japan", "JP", ContinentEnum.ASIA),
    KR("韩国", "South Korea", "KR", ContinentEnum.ASIA, "Korea", "Republic of Korea"),
    KP("朝鲜", "North Korea", "KP", ContinentEnum.ASIA, "Korea", "Democratic People's Republic of Korea", "DPRK"),
    SG("新加坡", "Singapore", "SG", ContinentEnum.ASIA),
    MY("马来西亚", "Malaysia", "MY", ContinentEnum.ASIA),
    TH("泰国", "Thailand", "TH", ContinentEnum.ASIA),
    VN("越南", "Vietnam", "VN", ContinentEnum.ASIA),
    PH("菲律宾", "Philippines", "PH", ContinentEnum.ASIA),
    ID("印度尼西亚", "Indonesia", "ID", ContinentEnum.ASIA),
    BN("文莱布鲁萨兰", "Brunei Darussalam", "BN", ContinentEnum.ASIA),
    KH("柬埔寨", "Cambodia", "KH", ContinentEnum.ASIA),
    LA("老挝", "Laos", "LA", ContinentEnum.ASIA),
    MM("缅甸", "Republic of the Union of Myanmar", "MM", ContinentEnum.ASIA, "Myanmar", "Burma"),
    TL("东帝汶", "East Timor", "TL", ContinentEnum.ASIA),
    IN("印度", "India", "IN", ContinentEnum.ASIA),
    PK("巴基斯坦", "Pakistan", "PK", ContinentEnum.ASIA),
    BD("孟加拉", "Bangladesh", "BD", ContinentEnum.ASIA),
    LK("斯里兰卡", "Sri Lanka", "LK", ContinentEnum.ASIA),
    NP("尼泊尔", "Nepal", "NP", ContinentEnum.ASIA),
    BT("不丹", "Bhutan", "BT", ContinentEnum.ASIA),
    MV("马尔代夫", "Maldives", "MV", ContinentEnum.ASIA),
    AF("阿富汗", "Afghanistan", "AF", ContinentEnum.ASIA),
    IR("伊朗", "Islamic Republic of Iran", "IR", ContinentEnum.ASIA, "Iran"),
    IQ("伊拉克", "Iraq", "IQ", ContinentEnum.ASIA),
    SY("叙利亚", "The Syrian Arab Republic", "SY", ContinentEnum.ASIA, "Syria"),
    JO("约旦", "Jordan", "JO", ContinentEnum.ASIA),
    LB("黎巴嫩", "Lebanon", "LB", ContinentEnum.ASIA),
    PS("巴勒斯坦", "Palestine", "PS", ContinentEnum.ASIA),
    IL("以色列", "Israel", "IL", ContinentEnum.ASIA),
    SA("沙特阿拉伯", "Saudi Arabia", "SA", ContinentEnum.ASIA),
    AE("阿联酋", "United Arab Emirates", "AE", ContinentEnum.ASIA),
    KW("科威特", "Kuwait", "KW", ContinentEnum.ASIA),
    BH("巴林", "Bahrain", "BH", ContinentEnum.ASIA),
    QA("卡塔尔", "Qatar", "QA", ContinentEnum.ASIA),
    OM("阿曼", "Oman", "OM", ContinentEnum.ASIA),
    YE("也门", "Yemen", "YE", ContinentEnum.ASIA),
    TR("土耳其", "Turkey", "TR", ContinentEnum.ASIA, "Türkiye", "Turkiye"),
    GE("格鲁吉亚", "Georgia", "GE", ContinentEnum.ASIA),
    AM("亚美尼亚", "Armenia", "AM", ContinentEnum.ASIA),
    AZ("阿塞拜疆", "Azerbaijan Republic", "AZ", ContinentEnum.ASIA),
    KZ("哈萨克斯坦", "Kazakhstan", "KZ", ContinentEnum.ASIA),
    UZ("乌兹别克斯坦", "Uzbekistan", "UZ", ContinentEnum.ASIA),
    TM("土库曼斯坦", "Turkmenistan", "TM", ContinentEnum.ASIA),
    KG("吉尔吉斯斯坦", "Kyrgyzstan", "KG", ContinentEnum.ASIA),
    TJ("塔吉克斯坦", "Tajikistan", "TJ", ContinentEnum.ASIA),
    MN("蒙古", "Mongolia", "MN", ContinentEnum.ASIA),
    XK("加罗林群岛", "Caroline Islands", "XK", ContinentEnum.ASIA),

    // 欧洲国家
    GB("英国", "United Kingdom", "GB", ContinentEnum.EUROPE, "UK", "Britain", "Great Britain"),
    UK("英国", "United Kingdom", "UK", ContinentEnum.EUROPE, "GB", "Britain", "Great Britain"),
    FR("法国", "France", "FR", ContinentEnum.EUROPE),
    DE("德国", "Germany", "DE", ContinentEnum.EUROPE),
    IT("意大利", "Italy", "IT", ContinentEnum.EUROPE),
    ES("西班牙", "Spain", "ES", ContinentEnum.EUROPE),
    PT("葡萄牙", "Portugal", "PT", ContinentEnum.EUROPE),
    NL("荷兰", "Netherlands", "NL", ContinentEnum.EUROPE),
    BE("比利时", "Belgium", "BE", ContinentEnum.EUROPE),
    LU("卢森堡", "Luxembourg", "LU", ContinentEnum.EUROPE),
    CH("瑞士", "Switzerland", "CH", ContinentEnum.EUROPE),
    AT("奥地利", "Austria", "AT", ContinentEnum.EUROPE),
    GR("希腊", "Greece", "GR", ContinentEnum.EUROPE),
    NO("挪威", "Norway", "NO", ContinentEnum.EUROPE),
    SE("瑞典", "Sweden", "SE", ContinentEnum.EUROPE),
    FI("芬兰", "Finland", "FI", ContinentEnum.EUROPE),
    DK("丹麦", "Denmark", "DK", ContinentEnum.EUROPE),
    IS("冰岛", "Iceland", "IS", ContinentEnum.EUROPE),
    IE("爱尔兰", "Ireland", "IE", ContinentEnum.EUROPE),
    PL("波兰", "Poland", "PL", ContinentEnum.EUROPE),
    CZ("捷克", "Czech Republic", "CZ", ContinentEnum.EUROPE),
    SK("斯洛伐克", "Slovakia", "SK", ContinentEnum.EUROPE),
    HU("匈牙利", "Hungary", "HU", ContinentEnum.EUROPE),
    RO("罗马尼亚", "Romania", "RO", ContinentEnum.EUROPE),
    BG("保加利亚", "Bulgaria", "BG", ContinentEnum.EUROPE),
    SI("斯洛文尼亚", "Slovenia", "SI", ContinentEnum.EUROPE),
    HR("克罗地亚", "Croatia", "HR", ContinentEnum.EUROPE),
    BA("波黑", "Bosnia and Herzegovina", "BA", ContinentEnum.EUROPE),
    RS("塞尔维亚", "Serbia", "RS", ContinentEnum.EUROPE),
    ME("黑山共和国", "Montenegro", "ME", ContinentEnum.EUROPE),
    YK("科索沃", "Kosovo", "YK", ContinentEnum.EUROPE),
    MK("马其顿", "Macedonia", "MK", ContinentEnum.EUROPE,"North Macedonia"),
    AL("阿尔巴尼亚", "Albania", "AL", ContinentEnum.EUROPE),
    LT("立陶宛", "Lithuania", "LT", ContinentEnum.EUROPE),
    LV("拉脱维亚", "Latvia", "LV", ContinentEnum.EUROPE),
    EE("爱沙尼亚", "Estonia", "EE", ContinentEnum.EUROPE),
    BY("白俄罗斯", "Belarus", "BY", ContinentEnum.EUROPE),
    UA("乌克兰", "Ukraine", "UA", ContinentEnum.EUROPE),
    MD("摩尔多瓦", "Moldova", "MD", ContinentEnum.EUROPE),
    RU("俄罗斯", "Russian Federation", "RU", ContinentEnum.EUROPE, "Russia"),
    MT("马耳他", "Malta", "MT", ContinentEnum.EUROPE),
    MC("摩纳哥", "Monaco", "MC", ContinentEnum.EUROPE),
    SM("圣马力诺", "San Marino", "SM", ContinentEnum.EUROPE),
    VA("梵蒂冈", "Vatican City State", "VA", ContinentEnum.EUROPE),
    LI("列支顿士登", "Liechtenstein", "LI", ContinentEnum.EUROPE),
    AD("安道尔", "Andorra", "AD", ContinentEnum.EUROPE),
    FO("法罗群岛", "Faroe Islands", "FO", ContinentEnum.EUROPE),
    GI("直布罗陀", "Gibraltar", "GI", ContinentEnum.EUROPE),
    GG("根西岛", "Guernsey", "GG", ContinentEnum.EUROPE),
    JE("泽西岛", "Jersey", "JE", ContinentEnum.EUROPE),
    IM("马恩岛", "Islan of Man", "IM", ContinentEnum.EUROPE),
    AX("奥兰群岛", "Aland Islands", "AX", ContinentEnum.EUROPE),
    SJ("斯瓦尔巴群岛和扬马延岛", "Svalbard and Jan Mayen", "SJ", ContinentEnum.EUROPE),
    TF("法属南部领地", "French Southern Territories", "TF", ContinentEnum.EUROPE),
    XJ("巴利阿里群岛", "Balearic Islands", "XJ", ContinentEnum.EUROPE),
    XH("亚速尔群岛", "Azores", "XH", ContinentEnum.EUROPE),
    XF("科西嘉岛", "Corsica", "XF", ContinentEnum.EUROPE),
    SX("荷属圣马丁", "SintMaarten", "SX", ContinentEnum.EUROPE),

    // 北美洲国家
    US("美国", "United States", "US", ContinentEnum.NORTH_AMERICA, "USA", "United States of America", "America"),
    CA("加拿大", "Canada", "CA", ContinentEnum.NORTH_AMERICA),
    MX("墨西哥", "Mexico", "MX", ContinentEnum.NORTH_AMERICA),
    CU("古巴", "The Republic of Cuba", "CU", ContinentEnum.NORTH_AMERICA),
    PA("巴拿马", "Panama", "PA", ContinentEnum.NORTH_AMERICA),
    CR("哥斯达黎加", "Costa Rica", "CR", ContinentEnum.NORTH_AMERICA),
    NI("尼加拉瓜", "Nicaragua", "NI", ContinentEnum.NORTH_AMERICA),
    HN("洪都拉斯", "Honduras", "HN", ContinentEnum.NORTH_AMERICA),
    SV("萨尔瓦多", "El Salvador", "SV", ContinentEnum.NORTH_AMERICA),
    GT("危地马拉", "Guatemala", "GT", ContinentEnum.NORTH_AMERICA),
    BZ("伯里兹", "Belize", "BZ", ContinentEnum.NORTH_AMERICA),
    HT("海地", "Haiti", "HT", ContinentEnum.NORTH_AMERICA),
    DO("多米尼加共和国", "Dominican Republic", "DO", ContinentEnum.NORTH_AMERICA),
    JM("牙买加", "Jamaica", "JM", ContinentEnum.NORTH_AMERICA),
    TT("特立尼达和多巴哥", "Trinidad and Tobago", "TT", ContinentEnum.NORTH_AMERICA),
    BS("巴哈马", "Bahamas", "BS", ContinentEnum.NORTH_AMERICA),
    BB("巴巴多斯", "Barbados", "BB", ContinentEnum.NORTH_AMERICA),
    GD("格林纳达", "Grenada", "GD", ContinentEnum.NORTH_AMERICA),
    LC("圣卢西亚", "Saint Lucia", "LC", ContinentEnum.NORTH_AMERICA),
    VC("圣文森特和格陵纳丁斯", "Saint Vincent and the Grenadines", "VC", ContinentEnum.NORTH_AMERICA),
    KN("圣基茨和尼维斯", "Saint Kitts-Nevis", "KN", ContinentEnum.NORTH_AMERICA),
    AG("安提瓜和巴布达", "Antigua and Barbuda", "AG", ContinentEnum.NORTH_AMERICA),
    DM("多米尼克", "Dominica", "DM", ContinentEnum.NORTH_AMERICA),
    PR("波多黎各", "Puerto Rico", "PR", ContinentEnum.NORTH_AMERICA),
    VI("美属维尔京群岛", "Virgin Islands U.S.", "VI", ContinentEnum.NORTH_AMERICA),
    VG("英属维尔京群岛", "British Virgin Islands", "VG", ContinentEnum.NORTH_AMERICA),
    AI("安圭拉岛", "Anguilla", "AI", ContinentEnum.NORTH_AMERICA),
    MS("蒙塞拉特岛", "Montserrat", "MS", ContinentEnum.NORTH_AMERICA),
    KY("开曼群岛", "Cayman Islands", "KY", ContinentEnum.NORTH_AMERICA),
    TC("特克斯和凯科斯群岛", "Turks and Caicos Islands", "TC", ContinentEnum.NORTH_AMERICA),
    BM("百慕大", "Bermuda", "BM", ContinentEnum.NORTH_AMERICA),
    GL("格陵兰岛", "Greenland", "GL", ContinentEnum.NORTH_AMERICA),
    PM("圣皮埃尔和密克隆岛", "Saint Pierre and Miquelon", "PM", ContinentEnum.NORTH_AMERICA),
    AW("阿鲁巴", "Aruba", "AW", ContinentEnum.NORTH_AMERICA),
    CW("库拉索", "Curacao", "CW", ContinentEnum.NORTH_AMERICA),
    AN("荷属安德列斯", "Netherlands Antilles", "AN", ContinentEnum.NORTH_AMERICA),
    GP("法属德洛普群岛", "Guadeloupe", "GP", ContinentEnum.NORTH_AMERICA),
    MQ("法属马提尼克群岛", "Martinique", "MQ", ContinentEnum.NORTH_AMERICA),
    MF("法属圣马丁", "Saint-Martin", "MF", ContinentEnum.NORTH_AMERICA),
    UM("美国外围岛屿", "United States Minor Outlying Islands", "UM", ContinentEnum.NORTH_AMERICA),
    HW("夏威夷", "Hawaii", "HW", ContinentEnum.NORTH_AMERICA),
    AK("阿拉斯加", "Alaska", "AK", ContinentEnum.NORTH_AMERICA),
    BQ("博奈尔岛", "Bonaire", "BQ", ContinentEnum.NORTH_AMERICA),
    XE("圣尤斯特歇斯岛", "Saint Eustatius", "XE", ContinentEnum.NORTH_AMERICA),
    XM("荷属圣马丁", "Sint Maarten", "XM", ContinentEnum.NORTH_AMERICA),
    XN("尼维斯岛", "Nevis", "XN", ContinentEnum.NORTH_AMERICA),

    // 南美洲国家
    BR("巴西", "Brazil", "BR", ContinentEnum.SOUTH_AMERICA),
    AR("阿根廷", "Argentina", "AR", ContinentEnum.SOUTH_AMERICA),
    CL("智利", "Chile", "CL", ContinentEnum.SOUTH_AMERICA),
    CO("哥伦比亚", "Colombia", "CO", ContinentEnum.SOUTH_AMERICA),
    VE("委内瑞拉", "Venezuela", "VE", ContinentEnum.SOUTH_AMERICA),
    PE("秘鲁", "Peru", "PE", ContinentEnum.SOUTH_AMERICA),
    EC("厄瓜多尔", "Ecuador", "EC", ContinentEnum.SOUTH_AMERICA),
    BO("玻利维亚", "Bolivia", "BO", ContinentEnum.SOUTH_AMERICA),
    PY("巴拉圭", "Paraguay", "PY", ContinentEnum.SOUTH_AMERICA),
    UY("乌拉圭", "Uruguay", "UY", ContinentEnum.SOUTH_AMERICA),
    GY("圭亚那", "Guyana", "GY", ContinentEnum.SOUTH_AMERICA),
    SR("苏里南", "Suriname", "SR", ContinentEnum.SOUTH_AMERICA),
    GF("法属圭亚那", "French Guiana", "GF", ContinentEnum.SOUTH_AMERICA),
    FK("福克兰群岛", "Falkland Islands", "FK", ContinentEnum.SOUTH_AMERICA),
    GS("南乔治亚岛和南桑威奇群岛", "South Georgia and The South Sandwich Islands", "GS", ContinentEnum.SOUTH_AMERICA),
    BL("圣巴托洛缪岛", "Saint-Barthélemy", "BL", ContinentEnum.SOUTH_AMERICA),

    // 非洲国家
    EG("埃及", "Egypt", "EG", ContinentEnum.AFRICA),
    ZA("南非", "South Africa", "ZA", ContinentEnum.AFRICA),
    NG("尼日利亚", "Nigeria", "NG", ContinentEnum.AFRICA),
    ET("埃塞俄比亚", "Ethiopia", "ET", ContinentEnum.AFRICA),
    KE("肯尼亚", "Kenya", "KE", ContinentEnum.AFRICA),
    TZ("坦桑尼亚", "Tanzania", "TZ", ContinentEnum.AFRICA),
    UG("乌干达", "Uganda", "UG", ContinentEnum.AFRICA),
    DZ("阿尔及利亚", "Algeria", "DZ", ContinentEnum.AFRICA),
    MA("摩洛哥", "Morocco", "MA", ContinentEnum.AFRICA),
    TN("突尼斯", "Tunisia", "TN", ContinentEnum.AFRICA),
    LY("利比亚", "Libya", "LY", ContinentEnum.AFRICA),
    SD("苏丹", "Sudan", "SD", ContinentEnum.AFRICA),
    SS("南苏丹", "South Sudan", "SS", ContinentEnum.AFRICA),
    SO("索马里", "Somalia", "SO", ContinentEnum.AFRICA),
    DJ("吉布提", "Djibouti", "DJ", ContinentEnum.AFRICA),
    ER("厄立特里亚", "Eritrea", "ER", ContinentEnum.AFRICA),
    GH("加纳", "Ghana", "GH", ContinentEnum.AFRICA),
    CI("科特迪瓦", "Ivory Coast", "CI", ContinentEnum.AFRICA),
    SN("塞内加尔", "Senegal", "SN", ContinentEnum.AFRICA),
    ML("马里", "Mali", "ML", ContinentEnum.AFRICA),
    BF("布基纳法索", "Burkina Faso", "BF", ContinentEnum.AFRICA),
    NE("尼日尔", "Niger", "NE", ContinentEnum.AFRICA),
    TD("乍得", "Chad", "TD", ContinentEnum.AFRICA),
    CM("喀麦隆", "Cameroon", "CM", ContinentEnum.AFRICA),
    CF("中非", "Central African Republic", "CF", ContinentEnum.AFRICA),
    CG("刚果", "The Republic of Congo", "CG", ContinentEnum.AFRICA),
    CD("刚果民主共和国", "Democratic Republic of the Congo", "CD", ContinentEnum.AFRICA),
    ZR("扎伊尔共和国", "The Republic of Zaire", "ZR", ContinentEnum.AFRICA),
    GA("加蓬", "Gabon Republic", "GA", ContinentEnum.AFRICA),
    GQ("赤道几内亚", "Equatorial Guinea", "GQ", ContinentEnum.AFRICA),
    ST("圣多美和普林西比", "Sao Tome and Principe", "ST", ContinentEnum.AFRICA),
    AO("安哥拉", "Angola", "AO", ContinentEnum.AFRICA),
    ZM("赞比亚", "Zambia", "ZM", ContinentEnum.AFRICA),
    ZW("津巴布韦", "Zimbabwe", "ZW", ContinentEnum.AFRICA),
    MW("马拉维", "Malawi", "MW", ContinentEnum.AFRICA),
    MZ("莫桑比克", "Mozambique", "MZ", ContinentEnum.AFRICA),
    NA("纳米比亚", "Namibia", "NA", ContinentEnum.AFRICA),
    BW("博茨瓦纳", "Botswana", "BW", ContinentEnum.AFRICA),
    LS("莱索托", "Lesotho", "LS", ContinentEnum.AFRICA),
    SZ("斯威士兰", "Swaziland", "SZ", ContinentEnum.AFRICA),
    MG("马达加斯加", "Madagascar", "MG", ContinentEnum.AFRICA),
    MU("毛里求斯", "Mauritius", "MU", ContinentEnum.AFRICA),
    SC("塞舌尔", "Seychelles", "SC", ContinentEnum.AFRICA),
    KM("科摩罗", "Comoros", "KM", ContinentEnum.AFRICA),
    RE("留尼旺岛", "Reunion", "RE", ContinentEnum.AFRICA),
    YT("马约特岛", "Mayotte", "YT", ContinentEnum.AFRICA),
    CV("佛得角", "Cape Verde Islands", "CV", ContinentEnum.AFRICA),
    GN("几内亚", "Guinea", "GN", ContinentEnum.AFRICA),
    GW("几内亚比绍", "Guinea-Bissau", "GW", ContinentEnum.AFRICA),
    SL("塞拉利昂", "Sierra Leone", "SL", ContinentEnum.AFRICA),
    LR("利比里亚", "Liberia", "LR", ContinentEnum.AFRICA),
    TG("多哥", "Togo", "TG", ContinentEnum.AFRICA),
    BJ("贝宁", "Benin", "BJ", ContinentEnum.AFRICA),
    GM("冈比亚", "Gambia", "GM", ContinentEnum.AFRICA),
    MR("毛里塔尼亚", "Mauritania", "MR", ContinentEnum.AFRICA),
    RW("卢旺达", "Rwanda", "RW", ContinentEnum.AFRICA),
    BI("布隆迪", "Burundi", "BI", ContinentEnum.AFRICA),
    SH("圣赫勒拿", "Saint Helena", "SH", ContinentEnum.AFRICA),
    EH("西撒哈拉", "Western Sahara", "EH", ContinentEnum.AFRICA),
    IC("加那利群岛", "Canary Islands", "IC", ContinentEnum.AFRICA),
    XI("马德拉群岛", "Madeira", "XI", ContinentEnum.AFRICA),
    XD("阿森松", "Ascension", "XD", ContinentEnum.AFRICA),
    XB("特里斯坦-达库尼亚群岛", "Tristan Da Cunha", "XB", ContinentEnum.AFRICA),

    // 大洋洲国家
    AU("澳大利亚", "Australia", "AU", ContinentEnum.OCEANIA),
    NZ("新西兰", "New Zealand", "NZ", ContinentEnum.OCEANIA),
    PG("巴布亚新几内亚", "Papua New Guinea", "PG", ContinentEnum.OCEANIA),
    FJ("斐济", "Fiji", "FJ", ContinentEnum.OCEANIA),
    SB("所罗门群岛", "Solomon Islands", "SB", ContinentEnum.OCEANIA),
    VU("瓦努阿鲁", "Vanuatu", "VU", ContinentEnum.OCEANIA),
    NC("新卡里多尼亚", "New Caledonia", "NC", ContinentEnum.OCEANIA),
    PF("法属玻里尼西亚", "French Polynesia", "PF", ContinentEnum.OCEANIA),
    WS("萨摩亚（西萨摩亚）", "Western Samoa", "WS", ContinentEnum.OCEANIA,"Samoa"),
    AS("东萨摩亚", "American Samoa", "AS", ContinentEnum.OCEANIA),
    TO("汤加", "Tonga", "TO", ContinentEnum.OCEANIA),
    KI("基里巴斯", "Kiribati", "KI", ContinentEnum.OCEANIA),
    TV("图瓦卢", "Tuvalu", "TV", ContinentEnum.OCEANIA),
    NR("瑙鲁", "Nauru", "NR", ContinentEnum.OCEANIA),
    PW("帕劳", "Palau", "PW", ContinentEnum.OCEANIA),
    FM("米克罗尼西亚", "Micronesia", "FM", ContinentEnum.OCEANIA),
    MH("马绍尔群岛", "Marshall Islands", "MH", ContinentEnum.OCEANIA),
    CK("库克群岛", "Cook Islands", "CK", ContinentEnum.OCEANIA),
    NU("纽爱", "Niue", "NU", ContinentEnum.OCEANIA),
    TK("托克劳", "Tokelau", "TK", ContinentEnum.OCEANIA),
    WF("瓦利斯群岛和富图纳群岛", "Wallis and Futuna", "WF", ContinentEnum.OCEANIA),
    PN("皮特凯恩群岛", "Pitcairn Islands", "PN", ContinentEnum.OCEANIA),
    GU("关岛", "Guam", "GU", ContinentEnum.OCEANIA),
    MP("北马里亚纳群岛", "Northern Mariana Islands", "MP", ContinentEnum.OCEANIA),
    NF("诺福克岛", "Norfolk Island", "NF", ContinentEnum.OCEANIA),
    CC("科科斯基林群岛", "Cocos Keeling Islands", "CC", ContinentEnum.OCEANIA),
    CX("圣诞岛", "Christmas Island", "CX", ContinentEnum.OCEANIA),
    XL("新西兰属土岛屿", "New Zealand Islands Territories", "XL", ContinentEnum.OCEANIA),

    // 南极洲
    BV("布韦岛", "Bouvet Island", "BV", ContinentEnum.ANTARCTICA),

    // 塞浦路斯特殊处理（CSV中标记为非洲，但通常认为是欧洲）
    CY("塞普路斯", "Cyprus", "CY", ContinentEnum.AFRICA);

    private final String nameCn;
    private final String nameEn;
    private final String shortCode;
    private final ContinentEnum continent;
    private final Set<String> aliases;

    // 静态缓存：规范化后的别名 -> 枚举映射
    private static final Map<String, CountryContinentEnum> NORMALIZED_ALIAS_MAP = new HashMap<>();

    static {
        for (CountryContinentEnum country : values()) {
            // 主名称
            NORMALIZED_ALIAS_MAP.put(normalizeForMatch(country.nameEn), country);
            // 所有别名
            for (String alias : country.aliases) {
                NORMALIZED_ALIAS_MAP.put(normalizeForMatch(alias), country);
            }
        }
    }

    CountryContinentEnum(String nameCn, String nameEn, String shortCode, ContinentEnum continent, String... aliases) {
        this.nameCn = nameCn;
        this.nameEn = nameEn;
        this.shortCode = shortCode;
        this.continent = continent;
        this.aliases = aliases == null ? Collections.emptySet() : Set.of(aliases);
    }

    public static CountryContinentEnum fromShortCode(String shortCode) {
        for (CountryContinentEnum country : values()) {
            if (country.shortCode.equalsIgnoreCase(shortCode)) {
                return country;
            }
        }
        return null;
    }

    public static CountryContinentEnum fromChineseName(String nameCn) {
        for (CountryContinentEnum country : values()) {
            if (country.nameCn.equals(nameCn)) {
                return country;
            }
        }
        return null;
    }

    /**
     * 根据英文名称查找国家（支持别名与模糊匹配）
     * @param nameEn 英文名称
     * @return 匹配的国家枚举，未找到返回 null
     */
    public static CountryContinentEnum fromEnglishName(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) {
            return null;
        }
        
        // 1. 精确匹配主名称
        for (CountryContinentEnum country : values()) {
            if (country.nameEn.equalsIgnoreCase(nameEn)) {
                return country;
            }
        }
        
        // 2. 精确匹配别名
        for (CountryContinentEnum country : values()) {
            for (String alias : country.aliases) {
                if (alias.equalsIgnoreCase(nameEn)) {
                    return country;
                }
            }
        }
        
        // 3. 规范化后模糊匹配（去除音标、大小写、空格等）
        String normalized = normalizeForMatch(nameEn);
        return NORMALIZED_ALIAS_MAP.get(normalized);
    }

    /**
     * 规范化字符串用于匹配：
     * - 转小写
     * - 去除音标符号（如 ü -> u）
     * - 去除空格、连字符、点号
     * - 统一常见变体（如 &amp; -> and）
     */
    private static String normalizeForMatch(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        String result = input.toLowerCase().trim();
        
        // Unicode 规范化（NFD 分解 + 去除音标）
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // 去除常见分隔符和标点
        result = result.replaceAll("[\\s\\-_.,']+", "");
        
        // 统一常见词汇变体
        result = result.replace("&", "and")
                .replace("republic", "")
                .replace("democratic", "")
                .replace("people's", "")
                .replace("peoples", "")
                .replace("federation", "")
                .replace("union", "")
                .replace("kingdom", "")
                .replace("state", "")
                .replace("island", "")
                .replace("the", "");
        
        return result;
    }
}
