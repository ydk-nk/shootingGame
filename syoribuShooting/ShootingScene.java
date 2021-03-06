package syoribuShooting;

import syoribuShooting.sprite.Animation;
import syoribuShooting.sprite.HitEffect1;
import syoribuShooting.sprite.Item;
import syoribuShooting.sprite.Target;
import syoribuShooting.system.Function;
import syoribuShooting.system.GifReader;
import syoribuShooting.system.MP3Player;
import syoribuShooting.system.StopWatch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import static syoribuShooting.GameConfig.*;

enum State
{
    WAIT_SHOOTING,
    SHOOTING,
    INTO_FEVER,
    TIME_OVER,
}

public class ShootingScene extends AbstractScene implements TargetEventListener
{
    private static final int TIME_LIMIT = 100 * 1000;
    private static final Shape CLIP_TIME_AREA = new RoundRectangle2D.Float(VIRTUAL_WIDTH-480, 14, 450, 75, 10, 10);
    private static final Color COLOR_TIME_AREA = new Color(255, 255, 255, 100);
    private final StopWatch stopWatch;
    private State state;
    private ScoreManager scoreManager;
    private TargetManager targetManager;
    private StageManager stageManager;
    private MP3Player bgm;
    private boolean timeVisible = true;
    private long startTime = -1;
    private int pulseCnt = 0;

    private static final Animation intoOverHeatAnim = new Animation(GifReader.readGif(GameConfig.getResourceAsStream(GameConfig.PATH_IMAGE + "overheat.gif")), 0, 0, false)
    {
        private static final int WAIT_CYCLE = 1;
        private int cycle = 0;
        @Override
        public void update()
        {
            if (cycle == 0) {
                addIndex(1);
            }
            if(getIndex() >= getFrameCount()-1) {
                setDisposed(true);
            }
            ++cycle;
            if (cycle >= WAIT_CYCLE) cycle = 0;
        }
    };

    private static final BufferedImage back_normal = GameConfig.readImage("back02.jpg");
    private static final BufferedImage back_fever  = GameConfig.readImage("back02-red.jpg");

    ShootingScene(final String filePath)
    {
        super();
        this.setBackImage(back_normal);
        this.stopWatch = new StopWatch();
        this.targetManager = new TargetManager(this);
        this.scoreManager = new ScoreManager()
        {
            @Override
            public void intoFeverMode()
            {
                bgm.stop();
                bgm = new MP3Player(bgm_overheat, true);
                new MP3Player(se_explosion, false);
                setState(State.INTO_FEVER);
                stop();
            }

            @Override
            public void intoNormalMode()
            {
                setBackImage(back_normal);
                bgm.stop();
                bgm = new MP3Player(bgm_shooting, true);
            }
        };
        this.stageManager = new StageManager();
        stageManager.readStageWithThread(getResourceAsStream(filePath));

        this.setState(State.WAIT_SHOOTING);
    }

    @Override
    public void initialize(Game game)
    {
        this.setState(State.WAIT_SHOOTING);
        this.stopWatch.initTimer(TIME_LIMIT);
        if (! Main.getCursorManager().changeCurrentCursor(ID_SHOOTING_CURSOR_NORMAL))
        {
            Main.getCursorManager().changeCurrentCursor(ID_CLEAR_CURSOR);
        }
        bgm = new MP3Player(bgm_shooting, true);
        game.setFPS(FPS);
    }

    @Override
    public void finish(Game game)
    {
        bgm.stop();
    }

    @Override
    public void update(final Game game, SceneManager.SceneChanger sceneChanger)
    {
        switch (this.getState()) {
            case WAIT_SHOOTING:
                this.setState(State.SHOOTING);
                this.stopWatch.startTimer();
                changeStage();
                break;

            case SHOOTING:
                updateShooting();
                break;

            case INTO_FEVER:
                if (intoOverHeatAnim.isDisposed()) {
                    intoOverHeatAnim.setIndex(0);
                    intoOverHeatAnim.setDisposed(false);
                    setState(State.SHOOTING);
                    restart();
                } else {
                    intoOverHeatAnim.update();
                    if (intoOverHeatAnim.getIndex() == 16) {
                        setBackImage(back_fever);
                    }
                }
                break;

            case TIME_OVER:
                sceneChanger.changeScene(new ResultScene(scoreManager.getResult()));
        }

        this.scoreManager.update(stopWatch.getElapsed());

        if (startTime >= 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (timeVisible) {
                if (elapsed > 70) {
                    timeVisible = false;
                    startTime = System.currentTimeMillis();
                    ++pulseCnt;
                }
            } else {
                if (elapsed > 70) {
                    timeVisible = true;
                    startTime = System.currentTimeMillis();
                    ++pulseCnt;
                }
            }
            if (pulseCnt > 14) {
                pulseCnt = 0;
                startTime = -1;
                timeVisible = true;
            }
        }
    }

    private void updateShooting()
    {
        if (this.stopWatch.isOverTimeLimit())
        {
            if (targetManager.isEmpty()) {
                this.setState(State.TIME_OVER);
            } else {
                targetManager.setAllState(Target.State.DISAPPEAR);
            }
        }
        if (mustFinishStage()) {
            targetManager.forEach(TargetManager.LOCAL_LIST, new Function<Target>()
            {
                @Override
                public void task(Target obj) { obj.setState(Target.State.DISAPPEAR); }
            });
            if (targetManager.isEmpty(TargetManager.LOCAL_LIST)) {
                changeStage();
            }
        }
        targetManager.update(getStageElapsedTime(), stopWatch.getElapsed());
    }

    @Override
    public void draw(final Graphics2D g2d)
    {
        g2d.drawImage(this.getBackImage(), 0, 0, GameConfig.VIRTUAL_WIDTH, GameConfig.VIRTUAL_HEIGHT, null);
        targetManager.draw(g2d);

        {
            Shape defaultClip = g2d.getClip();
            g2d.setClip(CLIP_TIME_AREA);
            g2d.setColor(COLOR_TIME_AREA);
            g2d.fill(CLIP_TIME_AREA);
            g2d.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 70));
            int t = this.stopWatch.getRemainTime();
            if (t < 0) t = 0;

            if (startTime >= 0) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.GREEN);
            }
            if (timeVisible) {
                g2d.drawString("Time: " + t / 1000 + "." + t % 1000 / 100, GameConfig.VIRTUAL_WIDTH - 450, 80);
            }

            g2d.setClip(defaultClip);
        }

        scoreManager.draw(g2d);
        if (getState() == State.INTO_FEVER) {
            intoOverHeatAnim.draw(g2d);
        }
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    private void changeStage()
    {
        final BaseStage stage = stageManager.getReadStage();

        stageManager.changeStage(stopWatch.getElapsed(), stage);
        targetManager.setLocalList(stage.getLocalTargetList());
        if (stage.getGlobalTargetList().size() > 0) {
            targetManager.getGlobalList().addAll(stage.getGlobalTargetList());
        }
        stageManager.readStageWithThread(
                getResourceAsStream(stage.getNextStageFilePath())
        );
        Runtime.getRuntime().gc();
    }

    private void stop()
    {
        this.stopWatch.stopTimer();
    }

    private void restart()
    {
        this.stopWatch.restartTimer();
    }

    private boolean mustFinishStage()
    {
        return targetManager.isEmpty(TargetManager.LOCAL_LIST) ||
                getStageElapsedTime() >= stageManager.getNowStage().getTimeLimit();
    }

    private int getStageElapsedTime()
    {
        return stageManager.getStageElapsedTime(stopWatch.getElapsed());
    }

    @Override
    public void cursorEnteredTarget()
    {
        Main.getCursorManager().changeCurrentCursor(ID_SHOOTING_CURSOR_GREEN);
    }

    @Override
    public void cursorExitedTarget()
    {
        Main.getCursorManager().changeCurrentCursor(ID_SHOOTING_CURSOR_NORMAL);
    }

    @Override
    public void clickedTarget(TargetEvent e)
    {
        new MP3Player(se_gun, false);

        scoreManager.notifyHitTarget(e);
        final Target target = e.getTarget();
        if (target != null)
        {
            target.setState(Target.State.CLICKED);
            Main.getAnimationProcessor().add( new HitEffect1(e.getMouseX(), e.getMouseY(), false) );
            if (target instanceof Item)
            {
                switch (target.getType()) {
                    case scoreUp:
                        new MP3Player(se_magical, false);
                        break;

                    case timeDecrease:
                        stopWatch.addRemainTime(-5 * 1000);
                        new MP3Player(se_down, false);
                        startTime = System.currentTimeMillis();
                        pulseCnt = 0;
                        break;
                }
            }
            else
            {
                Main.getAnimationProcessor().add( new HitEffect1(e.getMouseX(), e.getMouseY(), false) );
                new MP3Player(se_bomb, false);
            }
        }
    }
}
