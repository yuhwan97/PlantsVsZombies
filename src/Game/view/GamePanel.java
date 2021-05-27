package Game.view;

import Game.Collider;
import Game.LevelData;
import Pea.model.FreezePea;
import Pea.model.NormalPea;
import Pea.model.Pea;
import plant.model.FreezePeashooter;
import plant.model.Peashooter;
import plant.model.Plant;
import plant.model.Sunflower;
import zombie.model.ConeHeadZombie;
import zombie.model.NormalZombie;
import zombie.model.Zombie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends JLayeredPane {

    private static GamePanel gamePanel = null;

    private Image bgImage;
    private Image sunflowerImage;
    private Image peashooterImage;
    private Image freezePeashooterImage;

    private Image normalZombieImage;
    private Image coneHeadZombieImage;
    private Collider[] colliders;

    private ArrayList<ArrayList<Zombie>> laneZombies;
    private ArrayList<ArrayList<NormalPea>> lanePeas;

    private Timer redrawTimer;
    private Timer advancerTimer;
    private Timer zombieProducer;
    private JLabel sunScoreboard;

    private GameWindow.PlantType activePlantingBrush = GameWindow.PlantType.None;

    private int sunScore;


    private GamePanel() {
        JLabel sun = new JLabel("SUN");
        sun.setLocation(37, 80);
        sun.setSize(60, 20);

        initializeLayout(sun);
        loadImages();

        initializeLaneZombies();
        initializeLanePeas();
        initializeCollider();
        setSunScore(150);  //pool avalie


        setRedrawTimer();
        setAdvancerTimer();
        setZombieProducerTimer();
    }

    public static GamePanel getInstance(){
        if(gamePanel == null) {gamePanel=new GamePanel();}
        return gamePanel;
    }

    private void setZombieProducerTimer() {
        zombieProducer = new Timer(7000, (ActionEvent e) -> {
            Random rnd = new Random();
            LevelData lvl = new LevelData();
            String[] Level = lvl.LEVEL_CONTENT[Integer.parseInt(lvl.LEVEL_NUMBER) - 1];
            int[][] LevelValue = lvl.LEVEL_VALUE[Integer.parseInt(lvl.LEVEL_NUMBER) - 1];
            int lane = rnd.nextInt(5);
            int t = rnd.nextInt(100);
            Zombie zombie = null;
            for (int i = 0; i < LevelValue.length; i++) {
                if (t >= LevelValue[i][0] && t <= LevelValue[i][1]) {
                    zombie = Zombie.getZombie(Level[i], GamePanel.this, lane);
                }
            }
            addZombie(lane, zombie);
        });
        zombieProducer.start();
    }

	private void addZombie(int lane, Zombie zombie) {
		if(zombie!=null) {
			laneZombies.get(lane).add(zombie);
		}
	}

    private void setAdvancerTimer() {
        advancerTimer = new Timer(60, (ActionEvent e) -> advance());
        advancerTimer.start();
    }

    private void setRedrawTimer() {
        redrawTimer = new Timer(25, (ActionEvent e) -> {
            repaint();
        });
        redrawTimer.start();
    }

    private void initializeCollider() {
        colliders = new Collider[45];
        for (int i = 0; i < 45; i++) {
            Collider a = new Collider();
            a.setLocation(44 + (i % 9) * 100, 109 + (i / 9) * 120);
            a.setAction(new PlantActionListener((i % 9), (i / 9)));
            colliders[i] = a;
            add(a, new Integer(0));
        }
    }

    private void initializeLanePeas() {
        lanePeas = new ArrayList<>();
        lanePeas.add(new ArrayList<>()); //line 1
        lanePeas.add(new ArrayList<>()); //line 2
        lanePeas.add(new ArrayList<>()); //line 3
        lanePeas.add(new ArrayList<>()); //line 4
        lanePeas.add(new ArrayList<>()); //line 5
    }

    private void initializeLaneZombies() {
        laneZombies = new ArrayList<>();
        laneZombies.add(new ArrayList<>()); //line 1
        laneZombies.add(new ArrayList<>()); //line 2
        laneZombies.add(new ArrayList<>()); //line 3
        laneZombies.add(new ArrayList<>()); //line 4
        laneZombies.add(new ArrayList<>()); //line 5
    }

    private void initializeLayout(JLabel sunScoreboard) {
        setSize(1000, 752);
        setLayout(null);
        this.sunScoreboard = sunScoreboard;
        add(sunScoreboard, new Integer(2));
    }

    private void loadImages() {
        bgImage = new ImageIcon(this.getClass().getResource("../../images/mainBG.png")).getImage();

        peashooterImage = new ImageIcon(this.getClass().getResource("../../images/plants/peashooter.gif")).getImage();
        freezePeashooterImage = new ImageIcon(this.getClass().getResource("../../images/plants/freezepeashooter.gif")).getImage();
        sunflowerImage = new ImageIcon(this.getClass().getResource("../../images/plants/sunflower.gif")).getImage();
    }

    private void advance() {
        for (int laneIndex = 0; laneIndex < 5; laneIndex++) {
            zombieAdvance(laneIndex);
            
            peaAdvance(laneIndex);
            
            colliderAdvance();

        }
    }

	private void colliderAdvance() {
		for (Collider c: colliders) {
			if(c.getPlant()!=null && c.getPlant().getHealth() <= 0) {
				c.removePlant();
			}
		}
	}

	private void zombieAdvance(int laneIndex) {
		for (Zombie z : laneZombies.get(laneIndex)) {
		    z.advance();
		    if (z.getPosX() < 0) {
		    	gameOver();
		    }
		    if (!z.getAlive()) {
		    	killZombie(laneIndex, z);
		    	break;
		    }
		}
	}

    private void peaAdvance(int laneIndex) {
        for (int j = 0; j < lanePeas.get(laneIndex).size(); j++) {
            NormalPea pea = lanePeas.get(laneIndex).get(j);
            Rectangle peaRectangle = new Rectangle(pea.getXPosition(), 130 + pea.getMyLane() * 120, 28, 28);
            for (int zombieIndex = 0; zombieIndex < gamePanel.getLaneZombies().get(pea.getMyLane()).size(); zombieIndex++) {
                Zombie zombie = gamePanel.getLaneZombies().get(pea.getMyLane()).get(zombieIndex);
                Rectangle zombieRectangle = new Rectangle(zombie.getPosX(), 109 + pea.getMyLane() * 120, 400, 120);
                if (peaRectangle.intersects(zombieRectangle)) {
                    zombie.setHealth(zombie.getHealth() - 300);
                    if (pea instanceof FreezePea)
                        zombie.slow();
                    boolean exit = false;
                    
                    gamePanel.getLaneZombies().get(pea.getMyLane()).remove(pea);
                    if (exit) break;
                }
            }
            /*if(posX > 2000){
                gp.lanePeas.get(myLane).remove(this);
            }*/
            pea.advance();
        }
    }

	private void killZombie(int i, Zombie z) {
		System.out.println("ZOMBIE DIED");
		laneZombies.get(i).remove(z);
		setProgress(10);
	}

	private void gameOver() {
		JOptionPane.showMessageDialog(this, "ZOMBIES ATE YOUR BRAIN !" + '\n' + "Starting the level again");
		gamePanel = null;
		GameWindow.gw.dispose();
		GameWindow.gw = new GameWindow();
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bgImage, 0, 0, null);

        //Draw Plants
        for (int i = 0; i < 45; i++) {
            Collider c = colliders[i];
            if (c.getPlant() != null) {
                Plant p = c.getPlant();
                if (p instanceof Peashooter) {
                    g.drawImage(peashooterImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
                }
                if (p instanceof FreezePeashooter) {
                    g.drawImage(freezePeashooterImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
                }
                if (p instanceof Sunflower) {
                    g.drawImage(sunflowerImage, 60 + (i % 9) * 100, 129 + (i / 9) * 120, null);
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            for (Zombie zombie : laneZombies.get(i)) {
            	g.drawImage(Zombie.getImage(), zombie.getPosX(), 109 + (i * 120), null);
            }

            for (int j = 0; j < lanePeas.get(i).size(); j++) {
                NormalPea pea = lanePeas.get(i).get(j);
                g.drawImage(Pea.getImage(), pea.getXPosition(), 130 + (i * 120), null);
            }

        }
    }

    private class PlantActionListener implements ActionListener {

        int x, y;

        public PlantActionListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (activePlantingBrush == GameWindow.PlantType.Sunflower) {
                if (getSunScore() >= 50) {
                    colliders[x + y * 9].setPlant(new Sunflower( x, y));
                    setSunScore(getSunScore() - 50);
                }
            }
            if (activePlantingBrush == GameWindow.PlantType.Peashooter) {
                if (getSunScore() >= 100) {
                    colliders[x + y * 9].setPlant(new Peashooter( x, y));
                    setSunScore(getSunScore() - 100);
                }
            }

            if (activePlantingBrush == GameWindow.PlantType.FreezePeashooter) {
                if (getSunScore() >= 175) {
                    colliders[x + y * 9].setPlant(new FreezePeashooter( x, y));
                    setSunScore(getSunScore() - 175);
                }
            }
            activePlantingBrush = GameWindow.PlantType.None;
        }
    }

    static int progress = 0;

    public static void setProgress(int num) {
        progress = progress + num;
        System.out.println(progress);
        if (progress >= 150) {
            if ("1".equals(LevelData.LEVEL_NUMBER)) {
                JOptionPane.showMessageDialog(null, "LEVEL_CONTENT Completed !!!" + '\n' + "Starting next LEVEL_CONTENT");
                gamePanel = null;
                GameWindow.gw.dispose();
                LevelData.write("2");
                GameWindow.gw.gameStart();
            } else {
                JOptionPane.showMessageDialog(null, "LEVEL_CONTENT Completed !!!" + '\n' + "More Levels will come soon !!!" + '\n' + "Resetting data");
                LevelData.write("1");
                System.exit(0);
            }
            progress = 0;
        }
    }

    public GameWindow.PlantType getActivePlantingBrush() {
        return activePlantingBrush;
    }

    public void setActivePlantingBrush(GameWindow.PlantType activePlantingBrush) {
        this.activePlantingBrush = activePlantingBrush;
    }

    public ArrayList<ArrayList<Zombie>> getLaneZombies() {
        return laneZombies;
    }

    public void setLaneZombies(ArrayList<ArrayList<Zombie>> laneZombies) {
        this.laneZombies = laneZombies;
    }

    public ArrayList<ArrayList<NormalPea>> getLanePeas() {
        return lanePeas;
    }

    public void setLanePeas(ArrayList<ArrayList<NormalPea>> lanePeas) {
        this.lanePeas = lanePeas;
    }

    public Collider[] getColliders() {
        return colliders;
    }

    public void setColliders(Collider[] colliders) {
        this.colliders = colliders;
    }
    public int getSunScore() {
        return sunScore;
    }

    public void setSunScore(int sunScore) {
        this.sunScore = sunScore;
        sunScoreboard.setText(String.valueOf(sunScore));
    }
}
