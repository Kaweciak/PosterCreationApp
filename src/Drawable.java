import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

abstract class Drawable {
    AffineTransform transform;

    public abstract void Draw(Graphics2D g2d);

    public abstract boolean contains(Point p);

    public abstract Rectangle getBounds();

    public Point2D getCenter() {
        Rectangle bounds = getBounds();
        return new Point2D.Double(bounds.x + bounds.width / 2.0, bounds.y + bounds.height / 2.0);
    }

    public void translate(double dx, double dy) {
        AffineTransform translation = new AffineTransform();
        translation.setToTranslation(dx, dy);
        transform.preConcatenate(translation);
    }

    public void scale(double sx, double sy, double anchorX, double anchorY) {
        AffineTransform scaleTransform = new AffineTransform();
        scaleTransform.translate(anchorX, anchorY);
        scaleTransform.scale(sx, sy);
        scaleTransform.translate(-anchorX, -anchorY);
        transform.preConcatenate(scaleTransform);
    }

    public void rotate(double theta, double anchorX, double anchorY) {
        AffineTransform rotation = new AffineTransform();
        rotation.setToRotation(theta, anchorX, anchorY);
        transform.preConcatenate(rotation);
    }
}
