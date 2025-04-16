import java.awt.*;
import java.awt.geom.AffineTransform;

class ImageDrawable extends Drawable{
    Image image;

    public ImageDrawable(Image image, double x, double y) {
        this.image = image;
        this.transform = AffineTransform.getTranslateInstance(x, y);
    }

    @Override
    public void Draw(Graphics2D g2d) {
        g2d.drawImage(image, transform, null);
    }

    @Override
    public boolean contains(Point p) {
        Rectangle bounds = new Rectangle(0, 0, image.getWidth(null), image.getHeight(null));
        return transform.createTransformedShape(bounds).contains(p);
    }

    @Override
    public Rectangle getBounds() {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        Rectangle bounds = new Rectangle(0, 0, w, h);
        return transform.createTransformedShape(bounds).getBounds();
    }
}
