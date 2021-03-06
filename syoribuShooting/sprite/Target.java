package syoribuShooting.sprite;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public abstract class Target extends Sprite
{
    abstract protected void _update();

    public enum State
    {
        CREATED(false),
        ZOOM_UP(true),
        FLY(true),
        CLICKED(false),
        DISAPPEAR(true),
        DISPOSE(false);

        private final boolean clickable;

        State(boolean clickable) {
            this.clickable = clickable;
        }

        public boolean isClickable() { return this.clickable; }
    }

    private static final int MAX_ZOOM_UP = 100;
    private final TargetType type;
    private BufferedImage img;
    private Motion motion;
    private State state;
    private int zoomDelay;

    public Target(TargetType type, BufferedImage img, double centerX, double centerY, final Motion motion)
    {
        super(img.getWidth(), img.getHeight());

        this.type = type;
        this.img = img;
        this.motion = motion;
        this.setState(State.CREATED);
        this.setZoom(0);
        this.setZoomDelay(0);
        this.setCenterX(centerX);
        this.setCenterY(centerY);
    }

    public Target(TargetType type, BufferedImage img, double centerX, double centerY)
    {
        this(type, img, centerX, centerY, Motion.NO_MOVE);
    }

    public void update(int elapsedTime)
    {
        if (this.isDisposed())
        {
            this.setState(State.DISAPPEAR);
        }
        switch (this.getState()) {
            case CREATED:
                if (this.getState() == Target.State.CREATED && elapsedTime >= this.getZoomDelay())
                {
                    Point2D.Double p = this.getMotion().getStartPosition();
                    this.setXdefault(p.x);
                    this.setYdefault(p.y);
                    this.setState(Target.State.ZOOM_UP);
                }
                break;

            case ZOOM_UP:
                if (this.zoomUp(20) == false)
                {
                    this.setState(State.FLY);
                }
                break;

            case FLY:
                this.motion.move(elapsedTime);
                break;

            case CLICKED:
                this.disappearClicked();
                break;

            case DISAPPEAR:
                disappearMinimize();
                break;
        }
        _update();
    }

    public Bounds getBounds()
    {
        return new CircleBounds(this.getCenterX(), this.getCenterY(), this.getWidth() / 2 + 10);
    }

    public Bounds getCriticalBounds()
    {
        return new CircleBounds(getCenterX(), getCenterY(), getWidth() / 5);
    }

    public int getScore(int mouseX, int mouseY)
    {
        int score = getType().getDefaultScore();
        return (int)(score * (getCriticalBounds().isContain(mouseX, mouseY)? 1.5 : 1));
    }

    public boolean isClickable()
    {
        return getState().isClickable();
    }

    protected boolean zoomUp(int addition)
    {
        int zoom = this.getZoom();
        double prevCX = this.getCenterX();
        double prevCY = this.getCenterY();
        boolean canZoom = true;

        zoom += addition;

        if (zoom >= MAX_ZOOM_UP)
        {
            zoom = MAX_ZOOM_UP;
            canZoom = false;
        }
        this.setZoom(zoom);
        this.setCenterX(prevCX);
        this.setCenterY(prevCY);

        return canZoom;
    }

    protected void disappearClicked()
    {
        disappearMinimize();
    }

    protected void disappearMinimize()
    {
        int prevCX = (int)this.getCenterX();
        int prevCY = (int)this.getCenterY();

        if (getWidth() < 90)
        {
            setHeight(getHeight() + 30);
            if (getWidth() < 30)
            {
                this.setState(State.DISPOSE);
            }
        }
        setWidth(getWidth() / 2);
        setCenterX(prevCX);
        setCenterY(prevCY);
        setMotion(Motion.NO_MOVE);
    }

    public void draw(final Graphics2D g2d)
    {
        g2d.drawImage(this.img, (int)this.getX(), (int)this.getY(), this.getWidth(), this.getHeight(), null);
    }

    @Override
    public String toString()
    {
        return String.format("Target(%d, %d) state:%s,  zoomDelay:%dms, motion: %s",
                ((int) getXdefault()),((int) getYdefault()), getState().toString(), getZoomDelay(), getMotion());
    }

    @Override
    public double getXdefault()
    {
        return getCenterX();
    }

    @Override
    public double getYdefault()
    {
        return getCenterY();
    }

    @Override
    public void setXdefault(double x)
    {
        this.setCenterX(x);
    }

    @Override
    public void setYdefault(double y)
    {
        this.setCenterY(y);
    }

    public TargetType getType()
    {
        return this.type;
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    public void setMotion(final Motion motion)
    {
        this.motion = motion;
        motion.setSprite(this);
    }

    public Motion getMotion()
    {
        return this.motion;
    }

    public int getZoomDelay()
    {
        return zoomDelay;
    }

    public void setZoomDelay(int zoomDelay)
    {
        this.zoomDelay = zoomDelay;
    }
}
