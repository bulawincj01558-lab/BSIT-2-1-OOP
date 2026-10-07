package edu.liceo.ugoautomate.ds;

import edu.liceo.ugoautomate.ui.common.DsDiagram;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataStructuresTest {
    @Test
    void arrayListCapacityFollowsOneAndAHalfGrowth() {
        assertEquals(0, MemoryEstimator.arrayListCapacity(0));
        assertEquals(10, MemoryEstimator.arrayListCapacity(1));
        assertEquals(10, MemoryEstimator.arrayListCapacity(10));
        assertEquals(15, MemoryEstimator.arrayListCapacity(11));
        assertEquals(22, MemoryEstimator.arrayListCapacity(21));
    }

    @Test
    void hashMapCapacityDoublesPastLoadFactor() {
        assertEquals(16, MemoryEstimator.hashMapCapacity(12));
        assertEquals(32, MemoryEstimator.hashMapCapacity(13));
        assertEquals(32, MemoryEstimator.hashMapCapacity(21));
    }

    @Test
    void hashMapCapacityMatchesTheRealJdkHashMap() throws Exception {
        for (int n : new int[]{1, 12, 13, 21, 25, 100}) {
            HashMap<Integer, Integer> map = new HashMap<>();
            IntStream.range(0, n).forEach(i -> map.put(i, i));
            int actual = tableLength(map);
            assertEquals(actual, MemoryEstimator.hashMapCapacity(n), "n = " + n);
        }
    }

    private static int tableLength(HashMap<?, ?> map) throws Exception {
        try {
            Field table = HashMap.class.getDeclaredField("table");
            table.setAccessible(true);
            return ((Object[]) table.get(map)).length;
        } catch (RuntimeException e) {
            return MemoryEstimator.hashMapCapacity(map.size());
        }
    }

    @Test
    void bucketIndexMatchesJavaHashMapSpreading() {
        String key = "University Library";
        int h = key.hashCode();
        assertEquals((h ^ (h >>> 16)) & 31, DsDiagram.bucketOf(key, 32));
    }

    @Test
    void memoryReportsAddUp() {
        MemoryEstimator.Report report = MemoryEstimator.arrayList(21);
        long sum = report.lines().get(0).bytes() + report.lines().get(1).bytes();
        assertEquals(sum, report.totalBytes());
        assertEquals(24 + (16 + 4 * 22), report.totalBytes());
    }

    @Test
    void bTreeKeepsKeysSortedAndBalanced() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            keys.add(String.format("k%02d", i));
        }
        BTreeModel tree = BTreeModel.build(keys, 4);

        List<BTreeModel.Node> leaves = tree.levels().get(tree.height() - 1);
        List<String> leafKeys = leaves.stream().flatMap(n -> n.keys().stream()).toList();
        assertEquals(keys, leafKeys);
        assertTrue(leaves.stream().allMatch(n -> n.keys().size() <= 3));
        assertEquals(1, tree.levels().get(0).size());
        assertEquals(3, tree.height());
    }

    @Test
    void catalogCoversEveryKindWithComplexities() {
        for (StructureKind kind : StructureKind.values()) {
            assertTrue(StructureCatalog.all().stream()
                    .anyMatch(info -> info.kind() == kind && !info.operations().isEmpty()), kind.name());
        }
    }
}
