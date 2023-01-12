// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.freemarker;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFreemarker
{
    @Test
    public void testSimplePlaceHolder() throws Exception
    {
        String testQuery = "Select \"root\".date as \"date\" from testTable as \"root\" where \"date\" >'${date}'";
        Map vars = new HashMap<>();
        vars.put("date", "2018-10-15");
        String result = RelationalExecutor.process(testQuery, vars, "");
        Assert.assertEquals("\nSelect \"root\".date as \"date\" from testTable as \"root\" where \"date\" >'2018-10-15'", result);
    }

    @Test
    public void testSimplePlaceHolderObjectproperty() throws Exception
    {
        String testQuery = "Select \"root\".date as \"date\" from testTable as \"root\" where \"date\" >'${reportEndDate[\"day\"]}'";
        Map vars = new HashMap<>();
        vars.put("day", "2018-10-15");
        Map rootVars = new HashMap();
        rootVars.put("reportEndDate", vars);
        String result = RelationalExecutor.process(testQuery, rootVars, "");
        Assert.assertEquals("\nSelect \"root\".date as \"date\" from testTable as \"root\" where \"date\" >'2018-10-15'", result);
    }

    @Test
    public void testRenderCollectionOfIntegers() throws Exception
    {
        String query = "final collection : ${renderCollection(testCollection \',\')}";
        List collection = new ArrayList();
        collection.add(1);
        collection.add(2);
        Map rootMap = new HashMap();
        rootMap.put("testCollection", collection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : 1,2", result.trim());
    }

    @Test
    public void testRenderCollectionOfIntegersWithDefaultValue() throws Exception
    {
        String query = "final collection : ${renderCollectionWithDefaultValue(testCollection \",\" \"\" \"\" {} \"null\")}";
        List collection = new ArrayList();
        Map rootMap = new HashMap();
        rootMap.put("testCollection", collection);
        String resultWithEmptyCollection = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : null", resultWithEmptyCollection.trim());

        collection.add(1);
        collection.add(2);
        rootMap.put("testCollection", collection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : 1,2", result.trim());
    }

    @Test
    public void testRenderCollectionOfStrings() throws Exception
    {
        String query = "final collection :'${renderCollection(testCollection \"\',\'\")}'";
        List collection = new ArrayList();
        collection.add("a");
        collection.add("b");
        Map rootMap = new HashMap();
        rootMap.put("testCollection", collection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection :'a','b'", result.trim());
    }

    @Test
    public void testRenderCollectionOfStringsWithDefaultValue() throws Exception
    {
        String query = "final collection : ${renderCollectionWithDefaultValue(testCollection \",\" \"'\" \"'\" {\"'\" : \"''\"} \"null\")}";
        List collection = new ArrayList();
        Map rootMap = new HashMap();
        rootMap.put("testCollection", collection);
        String resultWithEmptyCollection = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : null", resultWithEmptyCollection.trim());

        collection.add("a");
        collection.add("b");
        collection.add("c'c");
        rootMap.put("testCollection", collection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : 'a','b','c''c'", result.trim());
    }

    @Test
    public void testRenderCollectionOfDateTimeWithDefaultValue() throws Exception
    {
        String query = "final collection : ${renderCollectionWithDefaultValue(testCollection \",\" \"convert(DATETIME, \'\" \"\', 121)\" {} \"null\")}";
        List collection = new ArrayList();
        Map rootMap = new HashMap();
        rootMap.put("testCollection", collection);
        String resultWithEmptyCollection = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : null", resultWithEmptyCollection.trim());

        collection.add("2020-12-12 00:0:00");
        collection.add("2020-12-12 20:00:00");
        rootMap.put("testCollection", collection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collection : convert(DATETIME, '2020-12-12 00:0:00', 121),convert(DATETIME, '2020-12-12 20:00:00', 121)", result.trim());
    }

    @Test
    public void testCollectionSizeTemplateFunction()
    {
        String query = "final collectionSize :${collectionSize(testCollection)}";

        List smallCollection = Lists.mutable.with(1, 2);
        Map rootMap = new HashMap();
        rootMap.put("testCollection", smallCollection);
        String result = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collectionSize :2", result.trim());

        List largeCollection = Lists.mutable.with(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 899, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997, 998, 999, 1000);
        rootMap.put("testCollection", largeCollection);
        String result1 = RelationalExecutor.process(query, rootMap, functionTemplates());
        Assert.assertEquals("final collectionSize :1000", result1.trim());
    }

    @Test
    public void testComputerNumberFormat()
    {
        String query = "final collectionSize :${testCollection?size}";

        List smallCollection = Lists.mutable.with(1, 2);
        Map rootMap = new HashMap();
        rootMap.put("testCollection", smallCollection);
        String result = RelationalExecutor.process(query, rootMap, "");
        Assert.assertEquals("final collectionSize :2", result.trim());

        List largeCollection = Lists.mutable.with(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 899, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943, 944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997, 998, 999, 1000);
        rootMap.put("testCollection", largeCollection);
        String result1 = RelationalExecutor.process(query, rootMap, "");
        Assert.assertEquals("final collectionSize :1000", result1.trim());
    }

    @Test
    public void testEnumPlaceHolderEqualInOp() throws Exception
    {
        String testQuery = "select distinct \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where ${equalEnumOperationSelector(enumMap_test_Map_CaseTypeMapping(cType), '\"root\".CASE_TYPE in (${enumMap_test_Map_CaseTypeMapping(cType)})', '\"root\".CASE_TYPE = ${enumMap_test_Map_CaseTypeMapping(cType)}')}";
        Map vars1 = new HashMap<>();
        vars1.put("cType", "Active");
        String result = RelationalExecutor.process(testQuery, vars1, functionTemplates());
        Assert.assertEquals("select distinct \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where \"root\".CASE_TYPE in ('A1', 'A2', 'A3')", result);
        Map vars2 = new HashMap<>();
        vars2.put("cType", "Deaths");
        result = RelationalExecutor.process(testQuery, vars2, functionTemplates());
        Assert.assertEquals("select distinct \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where \"root\".CASE_TYPE = 'D1'", result);
    }

    @Test
    public void testEnumPlaceHolderNotEqualInOp() throws Exception
    {
        String testQuery = "select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where ${equalEnumOperationSelector(enumMap_test_Map_CaseTypeMapping(cType), '(\"root\".CASE_TYPE not in (${enumMap_test_Map_CaseTypeMapping(cType)}) OR \"root\".CASE_TYPE is null)', '(\"root\".CASE_TYPE <> ${enumMap_test_Map_CaseTypeMapping(cType)} OR \"root\".CASE_TYPE is null)')}";
        Map vars1 = new HashMap<>();
        vars1.put("cType", "Active");
        String result = RelationalExecutor.process(testQuery, vars1, functionTemplates());
        Assert.assertEquals("select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where (\"root\".CASE_TYPE not in ('A1', 'A2', 'A3') OR \"root\".CASE_TYPE is null)", result);
        Map vars2 = new HashMap<>();
        vars2.put("cType", "Deaths");
        result = RelationalExecutor.process(testQuery, vars2, functionTemplates());
        Assert.assertEquals("select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where (\"root\".CASE_TYPE <> 'D1' OR \"root\".CASE_TYPE is null)", result);
    }

    @Test
    public void testEnumPlaceHolderMultipleEqualInOp() throws Exception
    {
        String testQuery = "select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where ${equalEnumOperationSelector(enumMap_test_Map_CaseTypeMapping(cType), '\"root\".CASE_TYPE in (${enumMap_test_Map_CaseTypeMapping(cType)})', '\"root\".CASE_TYPE = ${enumMap_test_Map_CaseTypeMapping(cType)}')} and ${equalEnumOperationSelector(enumMap_test_Map_CountryMapping(coType), '\"root\".COUNTRY in (${enumMap_test_Map_CountryMapping(coType)})', '\"root\".COUNTRY = ${enumMap_test_Map_CountryMapping(coType)}')}";
        Map vars = new HashMap<>();
        vars.put("cType", "Active");
        vars.put("coType", "AMEA");
        String result = RelationalExecutor.process(testQuery, vars, functionTemplates());
        Assert.assertEquals("select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where \"root\".CASE_TYPE in ('A1', 'A2', 'A3') and \"root\".COUNTRY in ('USA', 'America')", result);
    }

    @Test
    public void testEnumPlaceHolderMultipleNotEqualNotInOp() throws Exception
    {
        String testQuery = "select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where ${equalEnumOperationSelector(enumMap_test_Map_CaseTypeMapping(cType), '(\"root\".CASE_TYPE not in (${enumMap_test_Map_CaseTypeMapping(cType)}) OR \"root\".CASE_TYPE is null)', '(\"root\".CASE_TYPE <> ${enumMap_test_Map_CaseTypeMapping(cType)} OR \"root\".CASE_TYPE is null)')} and ${equalEnumOperationSelector(enumMap_test_Map_CountryMapping(coType), '(\"root\".COUNTRY not in (${enumMap_test_Map_CountryMapping(coType)}) OR \"root\".COUNTRY is null)', '(\"root\".COUNTRY <> ${enumMap_test_Map_CountryMapping(coType)} OR \"root\".COUNTRY is null)')}";
        Map vars = new HashMap<>();
        vars.put("cType", "Active");
        vars.put("coType", "AMEA");
        String result = RelationalExecutor.process(testQuery, vars, functionTemplates());
        Assert.assertEquals("select \"root\".CASE_TYPE as \"caseType\", \"root\".FIPS as \"fips\" from testTable as \"root\" where (\"root\".CASE_TYPE not in ('A1', 'A2', 'A3') OR \"root\".CASE_TYPE is null) and (\"root\".COUNTRY not in ('USA', 'America') OR \"root\".COUNTRY is null)", result);
    }

    @Test
    public void testEnumPlaceHolderIfOp() throws Exception
    {
        String testQuery1 = "select distinct case when ${equalEnumOperationSelector(enumMap_test_Map_CountryMapping(coType), 'EMEA in (${enumMap_test_Map_CountryMapping(coType)})', 'EMEA = ${enumMap_test_Map_CountryMapping(coType)}')} then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"";
        Map vars1 = new HashMap<>();
        vars1.put("coType", "EMEA");
        String result1 = RelationalExecutor.process(testQuery1, vars1, functionTemplates());
        Assert.assertEquals("select distinct case when EMEA = 'UK' then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"", result1);
        String testQuery2 = "select distinct case when ${equalEnumOperationSelector(enumMap_test_Map_CountryMapping(coType), 'AMEA in (${enumMap_test_Map_CountryMapping(coType)})', 'AMEA = ${enumMap_test_Map_CountryMapping(coType)}')} then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"";
        Map vars2 = new HashMap<>();
        vars2.put("coType", "AMEA");
        String result2 = RelationalExecutor.process(testQuery2, vars2, functionTemplates());
        Assert.assertEquals("select distinct case when AMEA in ('USA', 'America') then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"", result2);
        String testQuery3 = "select case when ${equalEnumOperationSelector(enumMap_test_Map_CountryMapping(coType), '\"root\".COUNTRY in (${enumMap_test_Map_CountryMapping(coType)})', '\"root\".COUNTRY = ${enumMap_test_Map_CountryMapping(coType)}')} then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"";
        Map vars3 = new HashMap<>();
        vars3.put("coType", "EMEA");
        String result3 = RelationalExecutor.process(testQuery3, vars3, functionTemplates());
        Assert.assertEquals("select case when \"root\".COUNTRY = 'UK' then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"", result3);
        String testQuery4 = "select case when '${coType}' = 'EMEA' then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"";
        Map vars4 = new HashMap<>();
        vars4.put("coType", "EMEA");
        String result4 = RelationalExecutor.process(testQuery4, vars4, functionTemplates());
        Assert.assertEquals("select case when 'EMEA' = 'EMEA' then \"root\".COUNTY else \"root\".COUNTY end as \"county\", \"root\".FIPS as \"fips\" from testTable as \"root\"", result4);
    }

    public static String functionTemplates()
    {
        return collectionTemplate() + "\n" + collectionSizeTemplate() + "\n" + renderCollectionWithDefaultTemplate() + "\n" + enumMap_test_Map_CaseTypeMapping() + "\n" +  enumMap_test_Map_CountryMapping() + "\n" + equalEnumOperationSelector();
    }
//corresponds to function templates coming with plan


    public static String collectionTemplate()
    {
        return "<#function renderCollection collection separator>" +
                "<#return collection?join(separator)>" +
                "</#function>"
                + "\n";
    }

    public static String equalEnumOperationSelector()
    {
        return  "<#function equalEnumOperationSelector enumVal inDyna equalDyna>" +
                    "<#assign enumList = enumVal?split(\",\")>" +
                    "<#if enumList?size = 1>" +
                        "<#return equalDyna>" +
                    "<#else>" +
                        "<#return inDyna></#if>" +
                "</#function>";
    }

    public static String enumMap_test_Map_CaseTypeMapping()
    {
        return "<#function enumMap_test_Map_CaseTypeMapping inputVal>" +
                "<#assign enumMap = { \"Active\":\"'A1', 'A2', 'A3'\", \"Deaths\":\"'D1'\", \"Confirmed\":\"'C1'\", \"Recovered\":\"'R1'\" }>" +
                "<#return enumMap[inputVal]>" +
                "</#function>";
    }

    public static String enumMap_test_Map_CountryMapping()
    {
        return "<#function enumMap_test_Map_CountryMapping inputVal>" +
                "<#assign enumMap = { \"EMEA\":\"'UK'\", \"AMEA\":\"'USA', 'America'\", \"ASIA\":\"'Asia'\" }>" +
                "<#return enumMap[inputVal]>" +
                "</#function>";
    }

    public static String renderCollectionWithDefaultTemplate()
    {
        return "<#function renderCollectionWithDefaultValue collection separator prefix suffix replacementMap defaultValue>" +
                "<#if collection?size == 0>" +
                "<#return defaultValue>" +
                "</#if>" +
                "<#assign newCollection = collection>" +
                "<#list replacementMap as oldValue, newValue>" +
                "   <#assign newCollection = collection?map(ele -> ele?replace(oldValue, newValue))>" +
                "</#list>" +
                "<#return prefix + newCollection?join(suffix + separator + prefix) + suffix>" +
                "</#function>";
    }

    public static String collectionSizeTemplate()
    {
        return "<#function collectionSize collection>" +
                "<#return collection?size?c> " +
                "</#function>";
    }

    @Test
    public void roleTransform() throws Exception
    {
        String template = "${transformRole(roleMap)?join(\",\")}";
        String func =
                "<#function transformRole roleMap>\n" +
                        "    <#assign roleList = []>\n" +
                        "        <#list roleMap as role>\n" +
                        "            <#assign roleList += [role[\"userRole\"]]>\n" +
                        "        </#list>\n" +
                        "   <#return roleList>\n" +
                        "</#function>";
        List maps = new ArrayList();
        Map a = new HashMap();
        a.put("userRole", "cadm");
        Map b = new HashMap();
        b.put("userRole", "sales");
        maps.add(a);
        maps.add(b);
        Map rootMap = new HashMap();
        rootMap.put("roleMap", maps);
        Assert.assertEquals("cadm,sales", RelationalExecutor.process(template, rootMap, func));

    }

    public Map getRootMap(String[] roles)
    {
        List roleList = new ArrayList();
        for (String role : roles)
        {
            Map roleMap = new HashMap();
            roleMap.put("userRole", role);
            roleList.add(roleMap);
        }
        Map rootMap = new HashMap();
        rootMap.put("role", roleList);
        return rootMap;
    }

    @Test
    public void testFreeMarkerInstanceOfMethod()
    {
        String testQuery = "Input is a stream - ${instanceOf(input, \"Stream\")?c}";

        Map<String, Result> vars = new HashMap<>();
        vars.put("input", new ConstantResult(Lists.mutable.with(1, 2).stream()));
        ExecutionState state = new ExecutionState(vars, Lists.mutable.empty(), Lists.mutable.empty());
        String result = FreeMarkerExecutor.process(testQuery, state);
        Assert.assertEquals("Input is a stream - true", result);

        Map<String, Result> vars1 = new HashMap<>();
        vars1.put("input", new ConstantResult(Lists.mutable.with(1, 2)));
        ExecutionState state1 = new ExecutionState(vars1, Lists.mutable.empty(), Lists.mutable.empty());
        String result1 = FreeMarkerExecutor.process(testQuery, state1);
        Assert.assertEquals("Input is a stream - false", result1);

        Map<String, Result> vars2 = new HashMap<>();
        vars2.put("input", new ConstantResult("1"));
        ExecutionState state2 = new ExecutionState(vars2, Lists.mutable.empty(), Lists.mutable.empty());
        String result2 = FreeMarkerExecutor.process(testQuery, state2);
        Assert.assertEquals("Input is a stream - false", result2);
    }
}
