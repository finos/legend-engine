package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.BufferedReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;

public class TestBufferedReader
{
    private BufferedReader buffer = new BufferedReader(10, 100, new StringReader("Life in the model world"));

    @Test
    public void canOpenACursorAndRead()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertEquals('L', cursor.advance());
        Assert.assertEquals('i', cursor.advance());
        Assert.assertEquals('f', cursor.advance());
        Assert.assertEquals('e', cursor.advance());
        Assert.assertArrayEquals(" in ".toCharArray(), cursor.advance(4));
    }

    @Test
    public void canOpenACursorAndPeek()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertEquals('L', cursor.peek(1));
        Assert.assertEquals('i', cursor.peek(2));
        Assert.assertEquals('f', cursor.peek(3));
        Assert.assertEquals('e', cursor.peek(4));
    }

    @Test
    public void canAdvanceACursorAndPeek()
    {
        CharCursor cursor = buffer.openCursor();
        cursor.advance(4);
        Assert.assertEquals(' ', cursor.peek(1));
        Assert.assertEquals('i', cursor.peek(2));
        Assert.assertEquals('n', cursor.peek(3));
        Assert.assertEquals(' ', cursor.peek(4));
    }

    @Test
    public void cannotOpenACursorOnceReadingHasBegun()
    {
        CharCursor cursor = buffer.openCursor();
        cursor.advance();
        try
        {
            buffer.openCursor();
            Assert.fail("Expected excpetion");
        }
        catch (IllegalStateException e)
        {
            Assert.assertEquals("Cannot open cursor once reading has begun", e.getMessage());
        }
    }

    @Test
    public void cursorsProgressIndependently()
    {
        CharCursor c1 = buffer.openCursor();
        CharCursor c2 = buffer.openCursor();

        Assert.assertArrayEquals("Life".toCharArray(), c1.advance(4));
        Assert.assertEquals('L', c2.advance());

        CharCursor c3 = c1.copy();
        c3.advance();

        Assert.assertArrayEquals(" in ".toCharArray(), c1.advance(4));
        Assert.assertArrayEquals("ife ".toCharArray(), c2.advance(4));
        Assert.assertArrayEquals("in t".toCharArray(), c3.advance(4));
    }

    @Test
    public void cursorsCanCauseMoreDataToEnterTheBuffer()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertArrayEquals("Life i".toCharArray(), cursor.advance(6));
        Assert.assertFalse(cursor.isEndOfData());
        Assert.assertArrayEquals("n the mod".toCharArray(), cursor.advance(9));
        Assert.assertArrayEquals("el ".toCharArray(), cursor.advance(3));
        Assert.assertArrayEquals("worl".toCharArray(), cursor.advance(4));
        Assert.assertEquals('d', cursor.advance());
        Assert.assertEquals(CharCursor.END_OF_DATA, cursor.advance());
        Assert.assertTrue(cursor.isEndOfData());
    }

    @Test
    public void peekingCanSeeEndOfData()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertArrayEquals("Life in the model ".toCharArray(), cursor.advance(18));
        Assert.assertFalse(cursor.isEndOfData());
    }

    @Test
    public void cursorsCanFillBufferWhenItOverflowsTheEnd()
    {
        CharCursor c1 = buffer.openCursor();
        CharCursor c2 = buffer.openCursor();

        // Advance cursors so that lowest is 8 chars consumed
        c1.advance(10);
        c2.advance(8);

        // Advance c1 further will fill buffer but only up to 8 chars because c2 prevents further
        c1.advance(5);

        // Destroy c2 to stop it preventig further consumption
        c2.destroy();

        // c1 should now be able to fill the remaining buffer and cycle round
        Assert.assertArrayEquals("el world".toCharArray(), c1.advance(999));
    }

    @Test
    public void cannotAdvanceCursorOnceDestroyed()
    {
        CharCursor cursor = buffer.openCursor();

        cursor.advance(10);
        cursor.destroy();
        try
        {
            cursor.advance();
            Assert.fail("Expected excpetion");
        }
        catch (IllegalStateException e)
        {
            Assert.assertEquals("This cursor has been destroyed", e.getMessage());
        }
    }

    @Test
    public void cannotAdvanceANegativeAmount()
    {
        CharCursor c1 = buffer.openCursor();
        try
        {
            c1.advance(-5);
            Assert.fail("Expected excpetion");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Cannot advance negatively", e.getMessage());
        }
    }

    @Test
    public void cannotPeekBackwards()
    {
        CharCursor c1 = buffer.openCursor();
        try
        {
            c1.peek(-1);
            Assert.fail("Expected excpetion");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Cannot peek on characters that have been advanced", e.getMessage());
        }
    }

    @Test
    public void cannotPeekCurrentPosition()
    {
        CharCursor c1 = buffer.openCursor();
        try
        {
            c1.peek(0);
            Assert.fail("Expected excpetion");
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Cannot peek on characters that have been advanced", e.getMessage());
        }
    }

    @Test
    public void canAdvanceByMoreThanTheBufferSize()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertArrayEquals("Life in the model".toCharArray(), cursor.advance(17));
    }

    @Test
    public void advancingBeyondEofReturnsWhatIsAvailable()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertArrayEquals("Life in the model ".toCharArray(), cursor.advance(18));
        Assert.assertArrayEquals("world".toCharArray(), cursor.advance(10));
    }

    @Test
    public void advancingAtEofReturnsEmptyArray()
    {
        CharCursor cursor = buffer.openCursor();
        Assert.assertArrayEquals("Life in the model world".toCharArray(), cursor.advance(23));
        Assert.assertTrue(cursor.isEndOfData());
        Assert.assertEquals(0, cursor.advance(10).length);
    }

    @Test
    public void capacityExceededIfPeekingTooFar()
    {
        try
        {
            buffer = new BufferedReader(5, 15, new StringReader("Life in the model world"));
            CharCursor cursor = buffer.openCursor();
            cursor.peek(16);
            Assert.fail("Exception expected");
        }
        catch (IllegalStateException e)
        {
            Assert.assertEquals("Insufficient capacity to load more data", e.getMessage());
        }

    }

    @Test
    public void capacityExceededIfConsumingTooFarOnOnlyOneCursor()
    {
        try
        {
            buffer = new BufferedReader(5, 15, new StringReader("Life in the model world"));
            CharCursor c1 = buffer.openCursor();
            CharCursor c2 = c1.copy();
            c2.advance(16);
            Assert.fail("Exception expected");
        }
        catch (IllegalStateException e)
        {
            Assert.assertEquals("Insufficient capacity to load more data", e.getMessage());
        }

    }
}
