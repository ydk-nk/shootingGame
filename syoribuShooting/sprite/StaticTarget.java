package syoribuShooting.sprite;

import syoribuShooting.Game;

import java.awt.image.BufferedImage;

public class StaticTarget extends Target
{
    private static final int SCORE = 100;

    @Override
    public int getScore(int screenX, int screenY)
    {
        return SCORE;
    }

    public StaticTarget(BufferedImage img, double centerX, double centerY)
    {
        super(img, centerX, centerY);
    }

    @Override
    public void update(final Game game)
    {
        super.update(game);
        switch (this.getState()) {
        case FLY:
            break;

        default:
            break;
        }
    }
}
