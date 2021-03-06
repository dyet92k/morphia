/**
 *
 */
package com.github.jmkgreen.morphia.optimisticlocks;

import java.util.ConcurrentModificationException;

import com.github.jmkgreen.morphia.mapping.MappedClass;
import org.junit.Assert;

import org.junit.Test;

import com.github.jmkgreen.morphia.TestBase;
import com.github.jmkgreen.morphia.annotations.Entity;
import com.github.jmkgreen.morphia.annotations.Id;
import com.github.jmkgreen.morphia.annotations.Version;
import com.github.jmkgreen.morphia.mapping.MappedField;
import com.github.jmkgreen.morphia.mapping.validation.ConstraintViolationException;
import com.github.jmkgreen.morphia.testutil.AssertedFailure;
import com.github.jmkgreen.morphia.testutil.TestEntity;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 *
 */
public class VersionTest extends TestBase {


	public static class ALongPrimitive extends TestEntity {
		private static final long serialVersionUID = 1L;

		@Version
		long hubba;

		String text;
	}

	public static class ALong extends TestEntity {
		private static final long serialVersionUID = 1L;
		@Version("versionNameContributedByAnnotation")
		Long v;

		String text;
	}

	@Entity
	static class InvalidVersionUse {
		@Id
		String id;
		@Version
		long version1;
		@Version
		long version2;

	}

	@Test
	public void testInvalidVersionUse() throws Exception {
		new AssertedFailure(ConstraintViolationException.class) {
			public void thisMustFail() throws Throwable {
				morphia.map(InvalidVersionUse.class);
			}
		};

	}

    @Test
    public void testLongPrimitiveHasVersioning() {
        ALongPrimitive a = new ALongPrimitive();
        MappedClass mc = morphia.map(ALongPrimitive.class).getMapper().getMappedClass(ALongPrimitive.class);
        Assert.assertTrue(mc.hasVersioning());

    }

	@Test
	public void testVersions() throws Exception {
		ALongPrimitive a = new ALongPrimitive();
		Assert.assertEquals(0, a.hubba);
		ds.save(a);
		Assert.assertTrue(a.hubba > 0);
		long version1 = a.hubba;

		ds.save(a);
		Assert.assertTrue(a.hubba > 0);
		long version2 = a.hubba;

		Assert.assertFalse(version1 == version2);
	}

	@Test
	public void testConcurrentModDetection() throws Exception {
		morphia.map(ALongPrimitive.class);

		ALongPrimitive a = new ALongPrimitive();
		Assert.assertEquals(0, a.hubba);
		ds.save(a);
		final ALongPrimitive a1 = a;

		ALongPrimitive a2 = ds.get(a);
		ds.save(a2);


		new AssertedFailure(ConcurrentModificationException.class) {
			public void thisMustFail() throws Throwable {
				ds.save(a1);
			}
		};
	}

	@Test
	public void testConcurrentModDetectionLong() throws Exception {
		ALong a = new ALong();
		Assert.assertEquals(null, (Long) a.v);
		ds.save(a);
		final ALong a1 = a;

		ALong a2 = ds.get(a);
		ds.save(a2);

		new AssertedFailure(ConcurrentModificationException.class) {
			public void thisMustFail() throws Throwable {
				ds.save(a1);
			}
		};
	}

	@Test
    public void testConcurrentModDetectionLongWithMerge() throws Exception {
        ALong a = new ALong();
        Assert.assertEquals(null, (Long) a.v);
        ds.save(a);
        final ALong a1 = a;

        a1.text = " foosdfds ";
        ALong a2 = ds.get(a);
        ds.save(a2);

        new AssertedFailure(ConcurrentModificationException.class) {
            public void thisMustFail() throws Throwable {
                ds.merge(a1);
            }
        };
    }

	@Test
	public void testVersionFieldNameContribution() throws Exception {
		MappedField mappedFieldByJavaField = morphia.getMapper().getMappedClass(ALong.class).getMappedFieldByJavaField("v");
		Assert.assertEquals("versionNameContributedByAnnotation", mappedFieldByJavaField.getNameToStore());
	}

}
