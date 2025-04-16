import java.awt.*;
import java.awt.geom.AffineTransform;

class ShapeDrawable extends Drawable{
    Shape originalShape;
    Color color;

    public ShapeDrawable(Shape shape, Color color) {
        this.originalShape = shape;
        this.color = color;
        this.transform = new AffineTransform();
    }

    @Override
    public void Draw(Graphics2D g2d) {
        Shape transformed = transform.createTransformedShape(originalShape);
        g2d.setColor(color);
        g2d.fill(transformed);
        g2d.setColor(Color.BLACK);
        g2d.draw(transformed);
    }

    @Override
    public boolean contains(Point p) {
        return transform.createTransformedShape(originalShape).contains(p);
    }

    @Override
    public Rectangle getBounds() {
        return transform.createTransformedShape(originalShape).getBounds();
    }
}