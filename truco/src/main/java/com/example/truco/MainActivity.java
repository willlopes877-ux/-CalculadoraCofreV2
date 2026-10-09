package com.example.truco;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new TrucoView(this));
    }

    static class Card {
        String rank, suit; int value;
        Card(String r, String s, int v) { rank=r; suit=s; value=v; }
        String label(){ return rank+suit; }
    }

    static class Player {
        String name; boolean human, teamA;
        ArrayList<Card> hand=new ArrayList<>();
        int mood=50;
        Player(String n, boolean h, boolean t){name=n;human=h;teamA=t;}
    }

    static class Played {
        int player; Card card; boolean covered;
        Played(int p, Card c){this(p,c,false);}
        Played(int p, Card c, boolean covered){player=p;card=c;this.covered=covered;}
    }

    static class TrucoView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Random rnd=new Random();
        ArrayList<Card> deck=new ArrayList<>();
        ArrayList<Played> trick=new ArrayList<>();
        ArrayList<Integer> trickWinners=new ArrayList<>();
        ArrayList<Card> seenCards=new ArrayList<>();
        Player[] players=new Player[4];
        Card vira;

        int teamAPoints=0, teamBPoints=0, stake=1, current=0, round=1, tricksA=0, tricksB=0, dealer=3, trickNo=1;
        boolean finished=false, waiting=false, elevenDecision=false, pendingRaise=false, showPartnerHand=false; boolean raiseByTeamA=false; int proposedStake=0;
        int screen=0; // 0 menu, 1 match, 2 how-to, 3 settings
        String status="Sua vez — escolha uma carta";
        RectF[] cardRects=new RectF[3];
        RectF trucoRect=new RectF(), restartRect=new RectF();
        RectF play11Rect=new RectF(), run11Rect=new RectF(); RectF acceptRect=new RectF(), foldRect=new RectF(), raiseRect=new RectF(), coverRect=new RectF();
        RectF playRect=new RectF(), howRect=new RectF(), settingsRect=new RectF(), backRect=new RectF(), menuGameRect=new RectF(), menuBackGameRect=new RectF(), restartGameRect=new RectF();
        RectF easyRect=new RectF(), normalRect=new RectF(), hardRect=new RectF();
        int difficulty=2; int aiStyle=2; String mode="PAULISTA"; RectF paulistaRect=new RectF(), mineiroRect=new RectF(); RectF aggressiveRect=new RectF(), strategistRect=new RectF(), blufferRect=new RectF();
        HashMap<String,Integer> rivalRaises=new HashMap<>(), rivalFolds=new HashMap<>(), rivalBluffs=new HashMap<>();
        ArrayList<String> trickHistory=new ArrayList<>();
        int opponentAggression=0, opponentFoldsToRaise=0, opponentSuccessfulBluffs=0;

        TrucoView(Context c){
            super(c);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));
            initPlayers();
        }

        void initPlayers(){
            players[0]=new Player("Você",true,true);
            players[1]=new Player("Parceira IA",false,true);
            players[2]=new Player("Rival IA",false,false);
            players[3]=new Player("Rival IA",false,false);
        }

        void startMatch(){
            teamAPoints=teamBPoints=0;
            round=1; finished=false; screen=1; dealer=3;
            newHand((dealer+1)%4);
        }

        void newHand(int first){
            deck.clear();
            trickHistory.clear(); opponentAggression=0; opponentFoldsToRaise=0; opponentSuccessfulBluffs=0; trick.clear(); trickWinners.clear(); seenCards.clear();
            stake=mode.equals("MINEIRO")?2:1; tricksA=tricksB=0; trickNo=1; waiting=false; elevenDecision=false; showPartnerHand=false; pendingRaise=false; proposedStake=0;
            for(Player pl:players) pl.hand.clear();
            buildDeck(); Collections.shuffle(deck,rnd);
            for(int i=0;i<3;i++) for(Player pl:players) pl.hand.add(deck.remove(0));
            vira=deck.remove(0); seenCards.add(vira); current=first;
            if(mode.equals("MINEIRO") && teamAPoints==10 && teamBPoints==10){
                stake=2;
                status="⚔️ MÃO DE FERRO — valendo 2!";
                invalidate();
            } else if(mode.equals("PAULISTA") && teamAPoints==11 && teamBPoints==11){
                stake=1;
                status="⚔️ MÃO DE FERRO — valendo 1!";
                invalidate();
            } else if((mode.equals("PAULISTA") && teamAPoints==11) || (mode.equals("MINEIRO") && teamAPoints==10)){
                elevenDecision=true; showPartnerHand=true;
                status=mode.equals("PAULISTA")?"MÃO DE 11 — veja as cartas da parceira e decida.":"MÃO DE 10 — veja as cartas da parceira e decida."; 
                invalidate(); return;
            }
            if(((mode.equals("PAULISTA") && teamBPoints==11) || (mode.equals("MINEIRO") && teamBPoints==10)) && teamAPoints<teamBPoints){
                // Os rivais decidem automaticamente com base nas cartas.
                if(aiAcceptTruco()){
                    stake=3;
                    status="MÃO DE 11 — rivais aceitaram jogar valendo 3.";
                }else{
                    teamAPoints++;
                    status="MÃO DE 11 — rivais correram. Sua dupla +1.";
                    if(teamAPoints>=12){ finished=true; invalidate(); return; }
                }
            }
            status=current==0?"Sua vez!":players[current].name+" começa.";
            invalidate();
            if(!players[current].human) runAITurn();
        }

        void buildDeck(){
            String[] s={"♣","♥","♠","♦"};
            String[] r={"4","5","6","7","Q","J","K","A","2","3"};
            int[] v={1,2,3,4,5,6,7,8,9,10};
            for(int a=0;a<4;a++) for(int i=0;i<r.length;i++) deck.add(new Card(r[i],s[a],v[i]));
        }

        String nextRank(String r){
            String[] rs={"4","5","6","7","Q","J","K","A","2","3"};
            for(int i=0;i<rs.length;i++) if(rs[i].equals(r)) return rs[(i+1)%rs.length];
            return "4";
        }

        int suitPower(String s){
            return s.equals("♣")?4:s.equals("♥")?3:s.equals("♠")?2:1;
        }

        int strength(Card c){
            if(mode.equals("MINEIRO")){
                if(c.rank.equals("4") && c.suit.equals("♣")) return 104;
                if(c.rank.equals("7") && c.suit.equals("♥")) return 103;
                if(c.rank.equals("A") && c.suit.equals("♠")) return 102;
                if(c.rank.equals("7") && c.suit.equals("♦")) return 101;
            }
            return c.rank.equals(nextRank(vira.rank)) ? 100+suitPower(c.suit) : c.value;
        }

        boolean manilha(Card c){
            if(mode.equals("MINEIRO")) return (c.rank.equals("4")&&c.suit.equals("♣")) || (c.rank.equals("7")&&c.suit.equals("♥")) || (c.rank.equals("A")&&c.suit.equals("♠")) || (c.rank.equals("7")&&c.suit.equals("♦"));
            return c.rank.equals(nextRank(vira.rank));
        }

        int publicCardsOfStrengthAtLeast(int min){
            int n=0;
            for(Card c:seenCards) if(strength(c)>=min) n++;
            return n;
        }

        int winProbability(Player pl){
            if(pl.hand.isEmpty()) return 0;
            int strong=0;
            for(Card c:pl.hand) if(strength(c)>=8) strong++;
            int max=0;
            for(Card c:pl.hand) max=Math.max(max,strength(c));
            int score=handScore(pl);
            int p=25 + strong*18 + Math.min(30,max>=100?30:max*2);
            if(score>=70) p+=10;
            if(teammateWinning(pl)) p+=12;
            if(trick.size()==0 && max>=100) p+=10;
            return Math.min(97,p);
        }

        int unseenCountForRank(String rank){ int n=0; for(Card c:deck) if(c.rank.equals(rank) && !seenCards.contains(c)) n++; return n; }

        boolean isLikelyBluff(Player pl){
            int score=handScore(pl); int win=winProbability(pl);
            return score<42 && win<58;
        }

        int strategicScore(Player pl){
            int base=handScore(pl), win=winProbability(pl);
            int pressure=pl.teamA?(teamAPoints-teamBPoints)*3:(teamBPoints-teamAPoints)*3;
            int aggression=opponentAggression*2-opponentFoldsToRaise;
            int styleBonus=aiStyle==1?12:aiStyle==2?8:16;
            return base+win/3+pressure+aggression+styleBonus;
        }

        String aiStyleName(){
            return aiStyle==1?"AGRESSIVA":aiStyle==2?"ESTRATEGISTA":"BLEFADORA";
        }

        int chooseProfessionalCard(Player pl){
            if(pl.hand.size()==1) return 0;
            final boolean aggressive=aiStyle==1, bluffer=aiStyle==3;

            // IA Mestre: primeiro entende o estado da vaza.
            int tableBest=-1;
            int tableWinner=-1;
            for(Played x:trick){
                if(x.covered) continue;
                int st=strength(x.card);
                if(st>tableBest){tableBest=st;tableWinner=x.player;}
            }

            // Parceiro ganhando: não desperdiça manilha/carta alta sem necessidade.
            if(tableWinner>=0 && players[tableWinner].teamA==pl.teamA && tableWinner!=current){
                int low=0;
                for(int i=1;i<pl.hand.size();i++)
                    if(strength(pl.hand.get(i))<strength(pl.hand.get(low))) low=i;
                if(!aggressive || rnd.nextInt(100)<82) return low;
            }

            // Se precisa ganhar, usa a menor carta que realmente mata a mesa.
            if(tableBest>=0){
                int best=-1, bestStrength=Integer.MAX_VALUE;
                for(int i=0;i<pl.hand.size();i++){
                    int st=strength(pl.hand.get(i));
                    if(st>tableBest && st<bestStrength){
                        bestStrength=st; best=i;
                    }
                }
                if(best>=0) return best;
            }

            // Abertura da vaza: cada personalidade joga de um jeito.
            if(trick.isEmpty()){
                if(aggressive){
                    int best=0;
                    for(int i=1;i<pl.hand.size();i++)
                        if(strength(pl.hand.get(i))>strength(pl.hand.get(best))) best=i;
                    return best;
                }
                if(bluffer && pl.hand.size()==3 && rnd.nextInt(100)<58){
                    // Carta média para não denunciar uma mão forte.
                    int mid=0;
                    for(int i=1;i<pl.hand.size();i++)
                        if(Math.abs(strength(pl.hand.get(i))-50)<Math.abs(strength(pl.hand.get(mid))-50)) mid=i;
                    return mid;
                }
            }

            int best=0;
            int bestCost=Integer.MAX_VALUE;
            for(int i=0;i<pl.hand.size();i++){
                Card c=pl.hand.get(i);
                int cost=strength(c);

                // Guarda manilha quando não é necessário gastar.
                if(manilha(c) && pl.hand.size()>1 && !aggressive) cost+=32;

                // Em apostas altas, pensa no valor da próxima vaza.
                if(stake>=6 && !aggressive && !matchPointFor(pl) && strength(c)>=60) cost+=18;

                // Blefadora prefere cartas intermediárias para manter a leitura difícil.
                if(bluffer && pl.hand.size()==3 && strength(c)>=20 && strength(c)<80) cost-=14;

                if(cost<bestCost){bestCost=cost;best=i;}
            }
            return best;
        }

        boolean matchPointFor(Player pl){
            int own=pl.teamA?teamAPoints:teamBPoints;
            return mode.equals("PAULISTA")?own>=10:own>=9;
        }

        int handScore(Player pl){
            int total=0, man=0;
            for(Card c:pl.hand){
                int st=strength(c); total+=st;
                if(manilha(c)) man++;
            }
            int score=pl.hand.isEmpty()?0:(total/pl.hand.size());
            if(man==1) score+=35;
            if(man>=2) score+=55;
            return score;
        }

        boolean teammateWinning(Player pl){
            if(trick.isEmpty()) return false;
            int best=-1, winner=-1;
            for(Played x:trick){
                int st=strength(x.card);
                if(st>best){best=st;winner=x.player;}
            }
            return winner>=0 && players[winner].teamA==pl.teamA && players[winner]!=pl;
        }

        Card chooseAI(Player pl){
            ArrayList<Card> sorted=new ArrayList<>(pl.hand);
            Collections.sort(sorted,(a,b)->Integer.compare(strength(a),strength(b)));
            if(sorted.isEmpty()) return null;

            // No modo DIFÍCIL, a IA Mestre usa leitura de mesa + estilo escolhido.
            if(difficulty>=3) return pl.hand.get(chooseProfessionalCard(pl));

            int own=pl.teamA?teamAPoints:teamBPoints;
            int opp=pl.teamA?teamBPoints:teamAPoints;
            int score=handScore(pl);
            boolean desperate=own<=3 && opp>=8;
            boolean matchPoint=own>=10;
            int bluffChance=difficulty==1?8:18;
            int preserve=difficulty==1?30:62;

            // Se o parceiro já está ganhando, economiza a carta e evita "matar o parceiro".
            if(teammateWinning(pl) && rnd.nextInt(100)<preserve) return sorted.get(0);

            int tableBest=-1;
            for(Played x:trick) if(!x.covered) tableBest=Math.max(tableBest,strength(x.card));

            // Sempre tenta ganhar gastando a menor carta possível.
            if(tableBest>=0){
                for(Card c:sorted) if(strength(c)>tableBest) return c;
                return sorted.get(0);
            }

            boolean hasMan=false;
            for(Card c:pl.hand) if(manilha(c)) hasMan=true;
            if(hasMan && (stake>=6 || matchPoint || desperate)) return sorted.get(sorted.size()-1);

            if(score>=45 && sorted.size()==3 && rnd.nextInt(100)<preserve) return sorted.get(1);
            if(sorted.size()==3 && rnd.nextInt(100)<bluffChance) return sorted.get(1);
            if(stake>=6 && !matchPoint && sorted.size()>1) return sorted.get(0);
            return sorted.get(0);
        }

        void playHuman(int idx){
            if(finished||waiting||elevenDecision||pendingRaise||current!=0||idx<0||idx>=players[0].hand.size()) return;
            Card c=players[0].hand.remove(idx);
            trick.add(new Played(0,c)); seenCards.add(c);
            status="Você jogou "+c.label()+" — carta na mesa!";
            invalidate();
            postDelayed(this::advanceTurn,220);
        }

        void advanceTurn(){
            if(trick.size()==4){ resolveTrick(); return; }
            current=(current+1)%4;
            if(players[current].human){
                status="Sua vez!";
                invalidate();
            } else runAITurn();
        }

        void runAITurn(){
            if(finished||waiting||elevenDecision||pendingRaise) return;
            Player pl=players[current];
            if(trick.size()==0 && !pl.human){ aiCallRaiseIfStrong(); if(pendingRaise) return; }
            if(pl.human){invalidate();return;}
            waiting=true;
            invalidate();
            postDelayed(()->{
                waiting=false;
                Card c=chooseAI(pl);
                if(c==null) return;
                pl.hand.remove(c);
                trick.add(new Played(current,c)); seenCards.add(c);
                String sig=current==1?partnerSignal(pl):rivalSignal(pl);
                status=sig.length()>0?pl.name+" fez o sinal "+sig:pl.name+" jogou "+c.label()+".";
                invalidate();
                postDelayed(this::advanceTurn,280);
            },520);
        }

        String partnerSignal(Player pl){
            if(pl.hand.isEmpty()) return "";
            Card best=Collections.max(pl.hand,(a,b)->Integer.compare(strength(a),strength(b)));
            if(manilha(best)){
                if(best.suit.equals("♣")) return "😉";
                if(best.suit.equals("♥")) return "😙";
                if(best.suit.equals("♠")) return "😬";
                return "👄";
            }
            return strength(best)>=9&&rnd.nextInt(100)<50?"🔥":"";
        }

        String rivalSignal(Player pl){
            int chance=difficulty==1?20:difficulty==2?32:45;
            if(aiStyle==1) chance+=8;
            if(aiStyle==3) chance+=22;
            if(rnd.nextInt(100)<Math.min(75,chance))
                return new String[]{"😉","😙","😬","👄"}[rnd.nextInt(4)];
            return "";
        }

        void resolveTrick(){
            trickHistory.add("v"+trickNo+":"+trick.size()+"@"+stake);
            int best=-1,bestStrength=-1; boolean tie=false;
            for(Played x:trick){
                if(x.covered) continue;
                int st=strength(x.card);
                if(st>bestStrength){bestStrength=st;best=x.player;tie=false;}
                else if(st==bestStrength) tie=true;
            }

            int winner;
            if(tie){
                // Paulista: empate na 1ª leva a decisão para a 2ª;
                // se já houve vencedor, a dupla que venceu a vaza anterior mantém a vantagem.
                winner=trickWinners.isEmpty()? -1 : trickWinners.get(trickWinners.size()-1);
            }else{
                winner=best;
            }

            // Se a primeira vaza empatou, ela não dá ponto de vaza.
            if(winner>=0){
                trickWinners.add(winner);
                if(players[winner].teamA) tricksA++; else tricksB++;
                status=(players[winner].teamA?"🤝 SUA DUPLA":"🤖 RIVAIS")+" ganharam a vaza!";
            }else{
                trickWinners.add(-1);
                status="⚔️ EMPATE! A próxima vaza decide.";
            }
            invalidate();

            trick.clear();
            trickNo++;
            if(tricksA>=2||tricksB>=2){
                boolean teamAWon=tricksA>=2;
                postDelayed(()->endHand(teamAWon),650);
                return;
            }

            // Quem ganhou a vaza começa a próxima. Em empate na primeira,
            // mantém-se a ordem original do primeiro jogador.
            if(winner>=0) current=winner;
            postDelayed(()->{
                if(current==0){status="Sua vez — próxima vaza.";invalidate();}
                else runAITurn();
            },700);
        }

        void endHand(boolean teamAWon){
            if(teamAWon) teamAPoints+=stake; else teamBPoints+=stake;
            if(teamAPoints>=12||teamBPoints>=12){
                finished=true;
                status=teamAPoints>=12?"🏆 VOCÊ + PARCEIRA VENCERAM!":"🤖 AS IAs RIVAIS VENCERAM!";
                invalidate(); return;
            }
            round++;
            dealer=(dealer+1)%4;
            newHand((dealer+1)%4);
        }

        boolean aiAcceptTruco(){
            // A decisão usa somente informação pública + a própria mão do decisor.
            // A IA não consulta as cartas do adversário.
            Player decider=(current>=0 && current<4 && !players[current].human)?players[current]:players[2];
            int score=handScore(decider);
            int own=decider.teamA?teamAPoints:teamBPoints;
            int opp=decider.teamA?teamBPoints:teamAPoints;
            int threshold= difficulty==1?58:difficulty==2?48:40;
            if(aiStyle==1) threshold-=10;
            if(aiStyle==2) threshold+=4;
            if(aiStyle==3) threshold-=6;
            if(mode.equals("MINEIRO")) threshold-=4;

            if(score>=threshold) return true;
            if((mode.equals("PAULISTA") && own>=10 && score<38) || (mode.equals("MINEIRO") && own>=10 && score<40)) return false;
            if(opp>=10 && score>=34) return true;

            // Blefe/coragem aumenta conforme a dificuldade e o valor em jogo.
            int bluff=difficulty==1?8:difficulty==2?18:30;
            if(aiStyle==1) bluff-=4;
            if(aiStyle==3) bluff+=22;
            if(stake>=6) bluff+=8;
            if(winProbability(decider)>=70) return true;
            return rnd.nextInt(100)<bluff;
        }

        void askTruco(){
            if(finished||waiting||elevenDecision||pendingRaise||current!=0||((mode.equals("PAULISTA")&&(teamAPoints==11||teamBPoints==11))||(mode.equals("MINEIRO")&&(teamAPoints==10||teamBPoints==10)))) return;
            int next=mode.equals("MINEIRO")?(stake==2?4:stake==4?6:stake==6?10:12):(stake==1?3:stake==3?6:stake==6?9:12);
            if(stake>=12){status="Já vale 12!";invalidate();return;}
            pendingRaise=true; raiseByTeamA=true; proposedStake=next;
            status="🔥 VOCÊ PEDIU "+(next==3?"TRUCO":next==6?"SEIS":next==9?"NOVE":"DOZE")+"!";
            invalidate(); postDelayed(()->aiRespondToRaise(),650);
        }

        void aiRespondToRaise(){
            if(!pendingRaise) return;
            if(aiAcceptTruco()){
                stake=proposedStake; pendingRaise=false; proposedStake=0;
                status="🤖 RIVAIS ACEITARAM! Agora vale "+stake+"."; invalidate();
            } else {
                pendingRaise=false; proposedStake=0; teamAPoints+=stake;
                status="🏃 RIVAIS CORRERAM! Sua dupla ganha "+stake+" ponto(s).";
                if(teamAPoints>=12){finished=true;invalidate();return;}
                round++; dealer=(dealer+1)%4; postDelayed(()->newHand((dealer+1)%4),650);
            }
        }

        void aiCallRaiseIfStrong(){
            if(finished||waiting||elevenDecision||pendingRaise||teamAPoints==11||teamBPoints==11||trick.size()>0) return;
            Player pl=players[current]; if(pl.human) return;
            int score=handScore(pl);
            int own=pl.teamA?teamAPoints:teamBPoints;
            int opp=pl.teamA?teamBPoints:teamAPoints;
            int chance=difficulty==1?7:difficulty==2?16:28;
            if(aiStyle==1) chance+=16;
            if(aiStyle==3) chance+=24;
            boolean strong=score>=48 || winProbability(pl)>=72;
            boolean pressure=own<opp && opp>=7;
            boolean bluff=score>=30 && rnd.nextInt(100)<chance;
            if(aiStyle==3 && score>=28 && rnd.nextInt(100)<38) bluff=true;
            if(aiStyle==2 && score<45) bluff=false;
            if(opp-own>=5) chance+=8;
            if(stake<12 && (strong || pressure || bluff)){
                int next=stake==1?3:stake==3?6:stake==6?9:12;
                // Em ponto de partida, só sobe muito com mão realmente forte.
                if(next>=9 && score<55 && own>=9) return;
                if(mode.equals("MINEIRO") && next>=10 && winProbability(pl)<82) return;
                pendingRaise=true; raiseByTeamA=false; proposedStake=next;
                status=pl.name+" pediu "+(next==3?"TRUCO":next==6?"SEIS":next==9?"NOVE":"DOZE")+"!";
                invalidate();
            }
        }

        void respondToAIRaise(boolean accept, boolean raise){
            if(!pendingRaise||raiseByTeamA)return;
            if(!accept&&!raise){
                pendingRaise=false; proposedStake=0; teamBPoints+=stake;
                status="🏃 VOCÊ CORREU! Rivais ganham "+stake+" ponto(s).";
                if(teamBPoints>=12){finished=true;invalidate();return;}
                round++;dealer=(dealer+1)%4;postDelayed(()->newHand((dealer+1)%4),650);return;
            }
            if(raise){
                if(proposedStake>=12){respondToAIRaise(true,false);return;}
                stake=proposedStake; proposedStake=proposedStake==3?6:proposedStake==6?9:12;
                raiseByTeamA=true; status="🔥 VOCÊ AUMENTOU PARA "+proposedStake+"!";
                invalidate(); postDelayed(()->aiRespondToRaise(),650); return;
            }
            stake=proposedStake; pendingRaise=false; proposedStake=0;
            status="Você aceitou. A mão vale "+stake+"."; invalidate(); postDelayed(()->runAITurn(),250);
        }

        void coverCard(){
            if(finished||waiting||elevenDecision||pendingRaise||current!=0||trickNo==1||players[0].hand.isEmpty())return;
            Card c=players[0].hand.remove(0); trick.add(new Played(0,c,true)); seenCards.add(c);
            status="Você jogou uma carta COBERTA. Ela não pode ganhar a vaza."; invalidate();
            postDelayed(this::advanceTurn,220);
        }

        void play11(){
            elevenDecision=false; stake=mode.equals("MINEIRO")?4:3;
            status=mode.equals("MINEIRO")?"🔥 VAMOS! Mão de 10 valendo 4.":"🔥 VAMOS! Mão de 11 valendo 3.";
            invalidate();
            if(current!=0) runAITurn();
        }

        void run11(){
            elevenDecision=false;
            teamBPoints += mode.equals("MINEIRO")?2:1;
            status=mode.equals("MINEIRO")?"🏃 CORRE! Rivais ganham 2 pontos.":"🏃 CORRE! Rivais ganham 1 ponto.";
            if(teamBPoints>=12){finished=true;invalidate();return;}
            round++;
            dealer=(dealer+1)%4;
            postDelayed(()->newHand((dealer+1)%4),650);
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            if(screen==0){drawMenu(c);return;}
            if(screen==2){drawHowTo(c);return;}
            if(screen==3){drawSettings(c);return;}
            drawGame(c);
        }

        void bg(Canvas c){
            c.drawColor(Color.rgb(12,17,14));
        }

        void button(Canvas c,RectF r,String text,int fill,float size){
            p.setColor(fill);c.drawRoundRect(r,18,18,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
            c.drawText(text,r.centerX(),r.centerY()-(p.ascent()+p.descent())/2,p);
        }

        void drawMenu(Canvas c){
            bg(c);
            float w=getWidth(),h=getHeight();
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.rgb(255,235,150));p.setTextSize(16);c.drawText("♠  ♥  TRUCO  ♦  ♣",w/2,92,p);
            p.setColor(Color.WHITE);p.setTextSize(48);c.drawText("TRUCO",w/2,150,p);
            p.setColor(Color.rgb(45,205,115));p.setTextSize(24);c.drawText("ARENA IA",w/2,183,p);
            p.setColor(Color.LTGRAY);p.setTextSize(14);c.drawText("PAULISTA  •  2x2  •  ESTRATÉGIA E BLEFE",w/2,211,p);

            playRect.set(w/2-190,250,w/2+190,330);
            howRect.set(w/2-145,350,w/2+145,405);
            settingsRect.set(w/2-145,423,w/2+145,478);
            button(c,playRect,"JOGAR "+mode,Color.rgb(35,145,78),26);
            button(c,howRect,"COMO JOGAR",Color.rgb(42,50,58),17);
            button(c,settingsRect,"CONFIGURAÇÕES",Color.rgb(42,50,58),17);

            p.setColor(Color.rgb(130,140,145));p.setTextSize(12);
            c.drawText("V2 • TRUCO "+mode+" • 2x2 • IA ESTRATÉGICA",w/2,h-35,p);
        }

        void drawHowTo(Canvas c){
            bg(c);float w=getWidth(),h=getHeight();
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("COMO JOGAR",w/2,65,p);
            p.setTextAlign(Paint.Align.LEFT);p.setTextSize(15);p.setColor(Color.LTGRAY);
            String[] lines={
                "• PAULISTA: 40 cartas, sem 8, 9 e 10; a VIRA define as manilhas.",
                "• PAULISTA: manilhas = carta seguinte à VIRA; ♣ > ♥ > ♠ > ♦.",
                "• PAULISTA: 1 → 3 → 6 → 9 → 12; Mão de 11 vale 3.",
                "• MINEIRO: manilhas fixas = 4♣ > 7♥ > A♠ > 7♦; não usa VIRA.",
                "• MINEIRO: mão começa em 2; pedidos tradicionais 4 → 6 → 10 → 12.",
                "• MINEIRO: Mão de 10 vale 4; Mão de Ferro é decisiva.",
                "• Ambos: 3 cartas, melhor de 3 vazas e partida até 12 pontos.",
                "• A IA conta cartas públicas, força a mesa, posição, placar e blefe.",
                "• Empates são tratados pela ordem das vazas e pela regra da variante."
            };
            float y=125;
            for(String s:lines){c.drawText(s,28,y,p);y+=43;}
            paulistaRect.set(28,395,w/2-8,448); mineiroRect.set(w/2+8,395,w-28,448);
            button(c,paulistaRect,"PAULISTA",mode.equals("PAULISTA")?Color.rgb(35,145,78):Color.rgb(42,50,58),15);
            button(c,mineiroRect,"MINEIRO",mode.equals("MINEIRO")?Color.rgb(35,145,78):Color.rgb(42,50,58),15);
            backRect.set(w/2-100,h-75,w/2+100,h-20);
            button(c,backRect,"VOLTAR",Color.rgb(42,50,58),17);
        }

        void drawSettings(Canvas c){
            bg(c);float w=getWidth(),h=getHeight();
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("CONFIGURAÇÕES",w/2,65,p);
            p.setTextSize(15);p.setColor(Color.LTGRAY);c.drawText("Dificuldade da IA",w/2,110,p);
            p.setTextSize(15);p.setColor(Color.LTGRAY);c.drawText("Estilo da IA Mestre",w/2,365,p);
            easyRect.set(28,145,w-28,198);normalRect.set(28,215,w-28,268);hardRect.set(28,285,w-28,338);
            button(c,easyRect,"FÁCIL",difficulty==1?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            button(c,normalRect,"NORMAL",difficulty==2?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            button(c,hardRect,"DIFÍCIL",difficulty==3?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            aggressiveRect.set(28,375,w-28,425); strategistRect.set(28,435,w-28,485); blufferRect.set(28,495,w-28,545);
            button(c,aggressiveRect,"⚡ AGRESSIVA",aiStyle==1?Color.rgb(175,55,45):Color.rgb(42,50,58),15);
            button(c,strategistRect,"🧠 ESTRATEGISTA",aiStyle==2?Color.rgb(35,145,78):Color.rgb(42,50,58),15);
            button(c,blufferRect,"🎭 BLEFADORA",aiStyle==3?Color.rgb(145,75,160):Color.rgb(42,50,58),15);
            p.setTextSize(12); p.setColor(Color.LTGRAY);
            c.drawText("Atual: "+aiStyleName(),w/2,570,p);
            backRect.set(w/2-100,h-75,w/2+100,h-20);
            button(c,backRect,"VOLTAR",Color.rgb(42,50,58),17);
        }

        void drawGame(Canvas c){
            float w=getWidth(),h=getHeight();
            c.drawColor(Color.rgb(30,126,68));

            // Campo verde com textura discreta, mantendo a mesa limpa e legível.
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(33,132,70));
            c.drawRect(0,0,w,h,p);
            p.setColor(Color.argb(22,255,255,255));
            for(int i=0;i<10;i++){
                float yy=170+i*(h-300)/10f;
                c.drawRect(0,yy,w,yy+((h-300)/20f),p);
            }
            p.setColor(Color.argb(35,0,35,15));
            c.drawRoundRect(10,165,w-10,h-160,35,35,p);

            // Cabeçalho escuro e compacto, com placar em destaque.
            float headerH=Math.max(145,h*0.115f);
            p.setColor(Color.rgb(13,25,45));
            c.drawRect(0,0,w,headerH,p);
            p.setColor(Color.rgb(27,42,64));
            c.drawRoundRect(10,9,57,48,12,12,p);
            menuGameRect.set(10,9,57,48);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(24);
            c.drawText("⚙",33.5f,37,p);
            menuBackGameRect.set(7,headerH-38,Math.min(92,w*.27f),headerH-7);
            restartGameRect.set(Math.min(98,w*.29f),headerH-38,Math.min(202,w*.57f),headerH-7);
            button(c,menuBackGameRect,"MENU",Color.rgb(43,58,78),11);
            button(c,restartGameRect,"REINICIAR",Color.rgb(43,58,78),10);

            float scoreY=headerH*.39f;
            float leftCenter=w*.25f,rightCenter=w*.75f;
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.rgb(255,221,102));p.setTextSize(Math.max(12,w*.027f));
            c.drawText("NÓS",leftCenter,scoreY,p);
            c.drawText("ELES",rightCenter,scoreY,p);
            p.setColor(Color.WHITE);p.setTextSize(Math.max(25,w*.065f));
            c.drawText(String.valueOf(teamAPoints),leftCenter,scoreY+34,p);
            c.drawText(String.valueOf(teamBPoints),rightCenter,scoreY+34,p);
            p.setColor(Color.rgb(170,190,210));p.setTextSize(18);c.drawText("×",w/2,scoreY+30,p);

            p.setColor(Color.rgb(215,225,238));p.setTextSize(Math.max(10,w*.025f));
            c.drawText("MÃO "+round+"  •  VALE "+stake+"  •  VAZAS "+tricksA+" × "+tricksB,w/2,headerH-45,p);
            p.setColor(Color.rgb(255,221,102));p.setTextSize(10);
            c.drawText("IA MESTRE: "+aiStyleName(),w/2,headerH-25,p);

            // Botões de acesso rápido ficam discretos no topo.
            float fieldTop=headerH+10;
            float avatarR=Math.min(35,w*.085f);
            float topY=fieldTop+avatarR+8;
            float sideY=Math.min(h*.43f,fieldTop+ h*.28f);
            drawOpponent(c,players[2],w/2,topY);
            drawTeammate(c,players[1],Math.max(avatarR+8,w*.12f),sideY);
            drawOpponent(c,players[3],Math.min(w-avatarR-8,w*.88f),sideY);

            // VIRA central, com contorno dourado e tamanho adaptado à tela.
            float vw=Math.min(88,w*.22f),vh=vw*1.34f;
            float vx=w/2-vw/2,vy=Math.max(fieldTop+avatarR*2+32,h*.405f);
            vy=Math.min(vy,h-350);
            p.setColor(Color.argb(100,0,20,10));
            c.drawRoundRect(vx-7,vy-7,vx+vw+7,vy+vh+7,14,14,p);
            p.setColor(Color.rgb(255,211,70));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);
            c.drawRoundRect(vx-4,vy-4,vx+vw+4,vy+vh+4,12,12,p);p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.rgb(255,230,130));p.setTextSize(13);
            c.drawText("VIRA",w/2,vy-12,p);
            if(vira!=null && mode.equals("PAULISTA")){
                int vc=(vira.suit.equals("♥")||vira.suit.equals("♦"))?Color.rgb(205,35,45):Color.rgb(20,25,25);
                p.setColor(vc);p.setTextSize(vw*.42f);c.drawText(vira.rank,w/2,vy+vh*.34f,p);
                p.setTextSize(vw*.52f);c.drawText(vira.suit,w/2,vy+vh*.70f,p);
                p.setColor(Color.rgb(255,230,130));p.setTextSize(10);
                c.drawText("MANILHA "+nextRank(vira.rank),w/2,vy+vh+17,p);
            } else if(mode.equals("MINEIRO")){
                p.setColor(Color.rgb(255,230,130));p.setTextSize(10);
                c.drawText("MANILHAS FIXAS",w/2,vy+vh+17,p);
            }

            // Área central onde as cartas jogadas permanecem visíveis.
            float tableTop=Math.min(h*.52f,vy+vh+36);
            p.setColor(Color.argb(55,0,25,12));
            c.drawRoundRect(12,tableTop,w-12,Math.min(h-220,tableTop+145),22,22,p);
            p.setColor(Color.WHITE);p.setTextSize(12);
            c.drawText("MESA  •  VAZA "+trickNo,w/2,tableTop-7,p);
            drawPlayedCards(c,w,h);

            p.setColor(Color.WHITE);p.setTextSize(12);
            c.drawText(status,w/2,Math.min(h-215,tableTop+155),p);

            // Cartas do jogador e ações na faixa inferior.
            float handY=h-190;
            drawHand(c,w/2,handY);
            trucoRect.set(w-112,h-104,w-10,h-48);
            button(c,trucoRect,"TRUCO!",Color.rgb(190,43,48),15);
            if(mode.equals("PAULISTA") && trickNo>1 && current==0 && !pendingRaise){
                coverRect.set(w/2-75,h-102,w/2+75,h-52);
                button(c,coverRect,"COBERTA",Color.rgb(58,76,97),13);
            }

            if(pendingRaise && !raiseByTeamA) drawRaiseDialog(c,w,h);
            if(elevenDecision) draw11(c,w,h);
            if(finished){
                restartRect.set(w/2-105,h-48,w/2+105,h-12);
                button(c,restartRect,"NOVA PARTIDA",Color.rgb(38,52,70),14);
            }
        }

        void drawPlayedCards(Canvas c,float w,float h){
            int n=trick.size();
            if(n==0){
                p.setColor(Color.argb(150,255,255,255));p.setTextSize(12);
                c.drawText("As cartas jogadas aparecem aqui",w/2,Math.min(h*.68f,h-245),p);
                return;
            }
            final float cw=Math.min(76,w*.19f),ch=cw*1.34f;
            float fieldTop=Math.max(160,h*.12f);
            float topY=fieldTop+Math.min(40,h*.035f);
            float sideY=Math.min(h*.55f, h*.43f);
            for(int i=0;i<n;i++){
                Played x=trick.get(i);
                float left,top;
                switch(x.player){
                    case 0:
                        left=w/2f-cw/2f;
                        top=Math.min(h-330,h*.68f);
                        break;
                    case 1:
                        left=Math.max(5,w*.12f-cw/2f);
                        top=sideY;
                        break;
                    case 2:
                        left=w/2f-cw/2f;
                        top=topY;
                        break;
                    default:
                        left=Math.min(w-cw-5,w*.88f-cw/2f);
                        top=sideY;
                        break;
                }
                p.setColor(Color.argb(150,0,0,0));
                c.drawRoundRect(left-3,top+4,left+cw+3,top+ch+5,10,10,p);
                if(x.covered) drawCoveredCard(c,left,top,cw,ch); else drawCard(c,x.card,left,top,cw,ch,true);
            }
        }

        void drawOpponent(Canvas c,Player pl,float x,float y){
            drawAvatar(c,x,y,pl==players[2]?2:3,50);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(13);
            c.drawText(pl.name,x,y+58,p);
            p.setColor(Color.rgb(220,235,225));p.setTextSize(12);
            c.drawText(pl.hand.size()+" cartas",x,y+74,p);
        }

        void drawTeammate(Canvas c,Player pl,float x,float y){
            drawAvatar(c,x,y,1,48);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(11);
            c.drawText("PARCEIRA IA",x,y+56,p);
            p.setColor(Color.rgb(220,235,225));p.setTextSize(10);
            c.drawText(pl.hand.size()+" cartas",x,y+71,p);
        }

        // Avatares desenhados no próprio jogo: não dependem de imagens externas.
        void drawAvatar(Canvas c,float cx,float cy,int type,float r){
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(210,0,0,0));c.drawCircle(cx,cy,r+5,p);
            int bg=type==1?Color.rgb(185,95,150):type==2?Color.rgb(65,120,190):type==3?Color.rgb(210,135,55):Color.rgb(75,165,105);
            p.setColor(bg);c.drawCircle(cx,cy,r,p);

            // pescoço
            p.setColor(Color.rgb(205,155,115));c.drawRoundRect(cx-9,cy+18,cx+9,cy+34,5,5,p);
            // roupa
            p.setColor(type==1?Color.rgb(55,65,115):type==2?Color.rgb(45,55,65):type==3?Color.rgb(115,55,45):Color.rgb(45,95,75));
            c.drawRoundRect(cx-25,cy+27,cx+25,cy+r+7,16,16,p);
            // rosto
            p.setColor(Color.rgb(222,170,125));c.drawCircle(cx,cy+2,r*0.58f,p);
            // cabelo
            p.setColor(type==1?Color.rgb(70,40,25):type==2?Color.rgb(45,30,20):type==3?Color.rgb(80,50,30):Color.rgb(30,25,20));
            if(type==1) c.drawArc(cx-r*0.58f,cy-r*0.48f,cx+r*0.58f,cy+20,180,180,true,p);
            else c.drawCircle(cx,cy-10,r*0.57f,p);
            // olhos
            p.setColor(Color.WHITE);c.drawCircle(cx-9,cy+2,4,p);c.drawCircle(cx+9,cy+2,4,p);
            p.setColor(Color.rgb(35,25,20));c.drawCircle(cx-9,cy+2,2,p);c.drawCircle(cx+9,cy+2,2,p);
            // boca / barba / detalhe
            if(type==2||type==3){
                p.setColor(Color.rgb(75,45,30));c.drawRoundRect(cx-10,cy+10,cx+10,cy+17,5,5,p);
            } else {
                p.setColor(Color.rgb(150,65,65));c.drawOval(cx-7,cy+10,cx+7,cy+15,p);
            }
            p.setStyle(Paint.Style.FILL);
        }

        void drawRaiseDialog(Canvas c,float w,float h){
            p.setColor(Color.argb(248,7,13,10)); c.drawRoundRect(15,h/2-95,w-15,h/2+105,22,22,p);
            p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(21);
            c.drawText("🔥 "+(proposedStake==3?"TRUCO":proposedStake==6?"SEIS":proposedStake==9?"NOVE":"DOZE")+"!",w/2,h/2-55,p);
            p.setTextSize(13); p.setColor(Color.LTGRAY); c.drawText("Rivais propõem "+proposedStake+" pontos.",w/2,h/2-30,p);
            acceptRect.set(28,h/2,w/2-12,h/2+50); foldRect.set(w/2+12,h/2,w-28,h/2+50);
            button(c,acceptRect,"ACEITAR",Color.rgb(35,135,75),14); button(c,foldRect,"CORRER",Color.rgb(150,45,45),14);
            if(proposedStake<12){raiseRect.set(28,h/2+62,w-28,h/2+110);button(c,raiseRect,"AUMENTAR",Color.rgb(185,110,35),14);}
        }

        void draw11(Canvas c,float w,float h){
            p.setColor(Color.argb(248,7,13,10));c.drawRoundRect(10,h/2-175,w-10,h/2+180,22,22,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(22);
            c.drawText(mode.equals("PAULISTA")?"MÃO DE 11":"MÃO DE 10",w/2,h/2-135,p);
            p.setTextSize(12);p.setColor(Color.LTGRAY);
            c.drawText("Sua dupla pode ver as cartas antes de decidir.",w/2,h/2-112,p);

            if(showPartnerHand && !players[1].hand.isEmpty()){
                p.setTextSize(11);p.setColor(Color.rgb(255,235,150));
                c.drawText("CARTAS DA PARCEIRA",w/2,h/2-88,p);
                float cw=72,ch=96,gap=58;
                float sx=w/2f-(players[1].hand.size()-1)*gap/2f-cw/2f;
                for(int i=0;i<players[1].hand.size();i++)
                    drawCard(c,players[1].hand.get(i),sx+i*gap,h/2-78,cw,ch,true);
            }

            p.setTextSize(13);p.setColor(Color.LTGRAY);
            c.drawText(mode.equals("PAULISTA")?"Jogar vale 3 pontos":"Jogar vale 4 pontos",w/2,h/2+34,p);
            c.drawText(mode.equals("PAULISTA")?"Correr entrega 1 ponto":"Correr entrega 2 pontos",w/2,h/2+54,p);
            play11Rect.set(w/2-145,h/2+72,w/2-10,h/2+122);
            run11Rect.set(w/2+10,h/2+72,w/2+145,h/2+122);
            button(c,play11Rect,mode.equals("PAULISTA")?"VAMOS! 3":"VAMOS! 4",Color.rgb(35,135,75),15);
            button(c,run11Rect,"CORRE",Color.rgb(150,45,45),15);
        }

        void drawHand(Canvas c,float x,float y){
            float w=getWidth();
            float cardW=Math.min(91,w*.235f),cardH=cardW*1.32f;
            float gap=Math.min(cardW*.88f,w*.205f);
            float count=players[0].hand.size();
            float start=x-(count-1)*gap/2f-cardW/2f;
            for(int i=0;i<count;i++){
                float left=start+i*gap;
                cardRects[i]=new RectF(left,y,left+cardW,y+cardH);
                drawCard(c,players[0].hand.get(i),left,y,cardW,cardH,false);
            }
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(11);
            c.drawText("SUAS CARTAS  •  TOQUE PARA JOGAR",x,y+cardH+15,p);
        }

        void drawCoveredCard(Canvas c,float x,float y,float cw,float ch){
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(25,55,90));
            c.drawRoundRect(x,y,x+cw,y+ch,13,13,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            p.setColor(Color.rgb(220,235,250));
            c.drawRoundRect(x+4,y+4,x+cw-4,y+ch-4,10,10,p);
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(24);
            c.drawText("🂠",x+cw/2,y+ch/2+8,p);
            p.setTextSize(10);
            p.setColor(Color.rgb(210,225,240));
            c.drawText("COBERTA",x+cw/2,y+ch-12,p);
        }

        void drawCard(Canvas c,Card card,float x,float y,float cw,float ch,boolean table){
            // Estilo de carta inspirado em apps modernos de truco: branca, limpa e muito legível.
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawRoundRect(x+2,y+4,x+cw+2,y+ch+5,13,13,p);

            p.setColor(Color.argb(80,0,0,0));
            c.drawRoundRect(x,y,x+cw,y+ch,13,13,p);

            p.setColor(Color.WHITE);
            c.drawRoundRect(x,y,x+cw,y+ch,13,13,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(table?3:2);
            p.setColor(Color.rgb(25,35,30));
            c.drawRoundRect(x,y,x+cw,y+ch,13,13,p);
            p.setStyle(Paint.Style.FILL);

            int textColor=(card.suit.equals("♥")||card.suit.equals("♦"))?Color.rgb(205,35,45):Color.rgb(20,25,25);
            p.setColor(textColor);p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans",Typeface.BOLD));
            p.setTextSize(table?28:32);c.drawText(card.rank,x+cw/2,y+38,p);
            p.setTextSize(table?38:44);c.drawText(card.suit,x+cw/2,y+82,p);
            p.setTextSize(table?9:10);p.setColor(Color.rgb(95,105,100));
            c.drawText("TRUCO",x+cw/2,y+ch-9,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX(),y=e.getY();

            if(screen==0){
                if(playRect.contains(x,y)){startMatch();return true;}
                if(howRect.contains(x,y)){screen=2;invalidate();return true;}
                if(settingsRect.contains(x,y)){screen=3;invalidate();return true;}
                return true;
            }
            if(screen==2){
                if(backRect.contains(x,y)){screen=0;invalidate();return true;}
                return true;
            }
            if(screen==3){
                if(easyRect.contains(x,y)){difficulty=1;invalidate();return true;}
                if(normalRect.contains(x,y)){difficulty=2;invalidate();return true;}
                if(hardRect.contains(x,y)){difficulty=3;invalidate();return true;}
                if(aggressiveRect.contains(x,y)){aiStyle=1;invalidate();return true;}
                if(strategistRect.contains(x,y)){aiStyle=2;invalidate();return true;}
                if(blufferRect.contains(x,y)){aiStyle=3;invalidate();return true;}
                if(paulistaRect.contains(x,y)){mode="PAULISTA";invalidate();return true;}
                if(mineiroRect.contains(x,y)){mode="MINEIRO";invalidate();return true;}
                if(backRect.contains(x,y)){screen=0;invalidate();return true;}
                return true;
            }

            if(finished&&restartRect.contains(x,y)){startMatch();return true;}
            if(menuGameRect.contains(x,y)||menuBackGameRect.contains(x,y)){screen=0;waiting=false;pendingRaise=false;elevenDecision=false;invalidate();return true;}
            if(restartGameRect.contains(x,y)){startMatch();return true;}
            if(elevenDecision){
                if(play11Rect.contains(x,y)){ showPartnerHand=false; play11(); }
                else if(run11Rect.contains(x,y)){ showPartnerHand=false; run11(); }
                return true;
            }
            if(pendingRaise && !raiseByTeamA){
                if(acceptRect.contains(x,y)) respondToAIRaise(true,false);
                else if(foldRect.contains(x,y)) respondToAIRaise(false,false);
                else if(raiseRect.contains(x,y)) respondToAIRaise(true,true);
                return true;
            }
            if(trucoRect.contains(x,y)){askTruco();return true;}
            if(coverRect.contains(x,y)){coverCard();return true;}
            if(current==0&&!waiting){
                for(int i=0;i<players[0].hand.size();i++){
                    if(cardRects[i]!=null&&cardRects[i].contains(x,y)){playHuman(i);return true;}
                }
            }
            return true;
        }
    }
}
