/*
 * InitializerBlock.java
 */

package jltools.ast;
import jltools.types.Context;
import jltools.util.CodeWriter;

/**
 * Overview: An InitializerBlock is a mutable representation of an
 * initializer block in a Java class (which appears outside of any
 * method).  Such a block is executed before the code for any of the
 * constructors.  Such a block can optionally be static, in which case
 * it is executed when the class is created.  
 */
public class InitializerBlock extends ClassMember {
  /**
   * Effects: Creates a new InitializerBlock containing <block>.
   * The InitializerBlock is static iff <isStatic> is true.
   */
   public InitializerBlock (BlockStatement block,
			    boolean isStatic) {
     this.block = block;
     this.isStatic = isStatic;
   }

  /**
   * Effects: Returns true iff this InitializerBlock contains static code.
   */
  public boolean isStatic() {
    return isStatic;
  }

  /**
   * Effects: Sets this InitializerBlock to contain static code if
   * <isStatic> is true, else the block contains non-static code.
   */
  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  /**
   * Effects: Returns the BlockStatement which comprises this
   * InitializerBlock.
   */
  public BlockStatement getBlock() {
    return block;
  }
  
  /**
   * Effects: Sets this InitializerBlock to contain <newBlock>.
   */
  public void setBlock(BlockStatement newBlock) {
    block = newBlock;
  }


  public void translate(Context c, CodeWriter w)
  {
    w.beginBlock();
    w.write (" { ");
    block.translate(c, w);
    w.write (" } ");
    w.endBlock();
  }

  public void dump(Context c, CodeWriter w)
  {
    w.write(" ( INITIALIZER BLOCK " );
    w.beginBlock();
    block.dump(c, w);
    w.endBlock();
    w.write(" )");
  }

  public Node typeCheck(Context c)
  {
    // FIXME; implement
    return this;
  }

  public void visitChildren(NodeVisitor v) {
    block = (BlockStatement) block.visit(v);
  }

  public Node copy() {
    InitializerBlock ib = new InitializerBlock(block, isStatic);
    ib.copyAnnotationsFrom(this);
    return ib;
  }

  public Node deepCopy() {
    InitializerBlock ib =
      new InitializerBlock((BlockStatement) block.deepCopy(), isStatic);
    ib.copyAnnotationsFrom(this);
    return ib;
  }

  private boolean isStatic;
  private BlockStatement block;
}


  
