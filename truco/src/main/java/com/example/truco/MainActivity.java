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
        int player; Card card;
        Played(int p, Card c){player=p;card=c;}
    }

    static class TrucoView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Random rnd=new Random();
        ArrayList<Card> deck=new ArrayList<>();
        ArrayList<Played> trick=new ArrayList<>();
        ArrayList<Integer> trickWinners=new ArrayList<>();
        Player[] players=new Player[4];
        Card vira;

        int teamAPoints=0, teamBPoints=0, stake=1, current=0, round=1, tricksA=0, tricksB=0, dealer=3, trickNo=1;
        boolean finished=false, waiting=false, elevenDecision=false;
        int screen=0; // 0 menu, 1 match, 2 how-to, 3 settings
        String status="Sua vez — escolha uma carta";
        RectF[] cardRects=new RectF[3];
        RectF trucoRect=new RectF(), restartRect=new RectF();
        RectF play11Rect=new RectF(), run11Rect=new RectF();
        RectF playRect=new RectF(), howRect=new RectF(), settingsRect=new RectF(), backRect=new RectF();
        RectF easyRect=new RectF(), normalRect=new RectF(), hardRect=new RectF();
        int difficulty=2;

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
            deck.clear(); trick.clear(); trickWinners.clear();
            stake=1; tricksA=tricksB=0; trickNo=1; waiting=false; elevenDecision=false;
            for(Player pl:players) pl.hand.clear();
            buildDeck(); Collections.shuffle(deck,rnd);
            for(int i=0;i<3;i++) for(Player pl:players) pl.hand.add(deck.remove(0));
            vira=deck.remove(0); current=first;
            if(teamAPoints==11 && teamBPoints==11){
                stake=1;
                status="⚔️ MÃO DE FERRO — valendo 1!";
                invalidate();
            } else if(teamAPoints==11){
                elevenDecision=true;
                status="MÃO DE 11 — sua dupla pode ver as cartas e decidir."; 
                invalidate(); return;
            }
            if(teamBPoints==11 && teamAPoints<11){
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
            return c.rank.equals(nextRank(vira.rank)) ? 100+suitPower(c.suit) : c.value;
        }

        boolean manilha(Card c){ return c.rank.equals(nextRank(vira.rank)); }

        Card chooseAI(Player pl){
            ArrayList<Card> sorted=new ArrayList<>(pl.hand);
            Collections.sort(sorted,(a,b)->Integer.compare(strength(a),strength(b)));
            if(sorted.isEmpty()) return null;

            int own=pl.teamA?teamAPoints:teamBPoints;
            int opp=pl.teamA?teamBPoints:teamAPoints;
            int strong=0, man=0;
            for(Card c:pl.hand){
                if(strength(c)>=8) strong++;
                if(manilha(c)) man++;
            }

            int bluffChance=difficulty==1?18:difficulty==2?28:38;
            int saveChance=difficulty==1?35:difficulty==2?55:72;

            // IA avalia estado da partida e não joga aleatoriamente.
            if(own+opp>=9 && own<opp) pl.mood=Math.min(100,pl.mood+12);
            if(own>opp+3) pl.mood=Math.max(0,pl.mood-7);

            if(man>0 && (stake==1 || pl.mood>68)) return sorted.get(sorted.size()-1);
            if(strong>=2 && stake==1 && rnd.nextInt(100)<saveChance)
                return sorted.get(sorted.size()-1);
            if(strong==1 && sorted.size()==3 && rnd.nextInt(100)<bluffChance)
                return sorted.get(1);

            // Se já há carta forte na mesa, tenta responder com o mínimo necessário.
            int tableBest=-1;
            for(Played x:trick) tableBest=Math.max(tableBest,strength(x.card));
            if(tableBest>=0){
                for(Card c:sorted) if(strength(c)>tableBest) return c;
            }

            // Guarda a melhor carta quando não precisa gastá-la.
            if(sorted.size()==3 && rnd.nextInt(100)<saveChance/2) return sorted.get(0);
            return sorted.get(rnd.nextInt(sorted.size()));
        }

        void playHuman(int idx){
            if(finished||waiting||elevenDecision||current!=0||idx<0||idx>=players[0].hand.size()) return;
            Card c=players[0].hand.remove(idx);
            trick.add(new Played(0,c));
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
            if(finished||waiting||elevenDecision) return;
            Player pl=players[current];
            if(pl.human){invalidate();return;}
            waiting=true;
            invalidate();
            postDelayed(()->{
                waiting=false;
                Card c=chooseAI(pl);
                if(c==null) return;
                pl.hand.remove(c);
                trick.add(new Played(current,c));
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
            if(rnd.nextInt(100)<chance)
                return new String[]{"😉","😙","😬","👄"}[rnd.nextInt(4)];
            return "";
        }

        void resolveTrick(){
            int best=-1,bestStrength=-1; boolean tie=false;
            for(Played x:trick){
                int st=strength(x.card);
                if(st>bestStrength){bestStrength=st;best=x.player;tie=false;}
                else if(st==bestStrength) tie=true;
            }

            // Regras de empate do Truco Paulista:
            // empate na 1a vaza: próxima vaza vale para decidir;
            // empate depois de uma vaza ganha: vence quem ganhou a anterior.
            int winner;
            if(tie){
                if(trickWinners.isEmpty()){
                    winner=current; // empate na primeira: mão segue para a próxima
                }else{
                    winner=trickWinners.get(trickWinners.size()-1);
                }
            }else{
                winner=best;
            }

            trickWinners.add(winner);
            if(players[winner].teamA) tricksA++; else tricksB++;
            status=(players[winner].teamA?"🤝 SUA DUPLA":"🤖 RIVAIS")+" ganharam a vaza!";
            invalidate();

            trick.clear();
            trickNo++;
            if(tricksA>=2||tricksB>=2){
                postDelayed(()->endHand(players[winner].teamA),650);
                return;
            }
            current=winner;
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
            int top=0,strong=0,man=0;
            for(Player pl:players){
                if(pl.teamA) continue;
                for(Card c:pl.hand){
                    top=Math.max(top,strength(c));
                    if(strength(c)>=8) strong++;
                    if(manilha(c)) man++;
                }
            }
            int chance=difficulty==1?20:difficulty==2?32:48;
            if(man>0||strong>=2||top>=9) return true;
            return rnd.nextInt(100)<(chance+(stake>=6?10:0));
        }

        void askTruco(){
            if(finished||waiting||elevenDecision||current!=0||teamAPoints==11||teamBPoints==11) return;
            int next=stake==1?3:stake==3?6:stake==6?9:12;
            if(stake>=12){status="Já vale 12!";invalidate();return;}

            // No Paulista, o aumento é 3, 6, 9 e 12.
            // Se os rivais não aceitam, a dupla que pediu ganha o valor anterior.
            if(aiAcceptTruco()){
                stake=next;
                status="🔥 RIVAIS ACEITARAM! Agora vale "+stake+".";
            }else{
                teamAPoints+=stake;
                status="🏃 RIVAIS CORRERAM! Sua dupla ganha "+stake+" ponto(s).";
                if(teamAPoints>=12) finished=true;
                else {
                    round++;
                    dealer=(dealer+1)%4;
                    postDelayed(()->newHand((dealer+1)%4),650);
                }
            }
            invalidate();
        }

        void play11(){
            elevenDecision=false; stake=3;
            status="🔥 VAMOS! Mão de 11 valendo 3.";
            invalidate();
            if(current!=0) runAITurn();
        }

        void run11(){
            elevenDecision=false;
            teamBPoints++;
            status="🏃 CORRE! Rivais ganham 1 ponto.";
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
            p.setColor(Color.WHITE);p.setTextSize(38);c.drawText("TRUCO",w/2,145,p);
            p.setColor(Color.rgb(45,185,105));p.setTextSize(22);c.drawText("ARENA IA",w/2,178,p);
            p.setColor(Color.LTGRAY);p.setTextSize(14);c.drawText("TRUCO PAULISTA • estratégia • blefe",w/2,205,p);

            playRect.set(w/2-145,270,w/2+145,332);
            howRect.set(w/2-145,350,w/2+145,405);
            settingsRect.set(w/2-145,423,w/2+145,478);
            button(c,playRect,"JOGAR",Color.rgb(35,145,78),20);
            button(c,howRect,"COMO JOGAR",Color.rgb(42,50,58),17);
            button(c,settingsRect,"CONFIGURAÇÕES",Color.rgb(42,50,58),17);

            p.setColor(Color.rgb(130,140,145));p.setTextSize(12);
            c.drawText("V2 • TRUCO PAULISTA • 2x2",w/2,h-35,p);
        }

        void drawHowTo(Canvas c){
            bg(c);float w=getWidth(),h=getHeight();
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("COMO JOGAR",w/2,65,p);
            p.setTextAlign(Paint.Align.LEFT);p.setTextSize(15);p.setColor(Color.LTGRAY);
            String[] lines={
                "• Truco Paulista: 40 cartas, sem 8, 9 e 10.",
                "• 3 cartas por jogador; a VIRA define a manilha.",
                "• Força: manilhas > 3 > 2 > A > K > J > Q > 7 > 6 > 5 > 4.",
                "• Manilhas: ♣ Zap > ♥ > ♠ > ♦.",
                "• Melhor de 3 vazas; empate segue a regra da vaza anterior.",
                "• Valor: 1 → 3 → 6 → 9 → 12. Pode aceitar, correr ou aumentar.",
                "• Mão de 11 vale 3 se jogar e não permite Truco.",
                "• 11×11 é Mão de Ferro: vale 1 e decide a partida.",
                "• A carta coberta não pode ser usada na primeira vaza."
            };
            float y=125;
            for(String s:lines){c.drawText(s,28,y,p);y+=43;}
            backRect.set(w/2-100,h-75,w/2+100,h-20);
            button(c,backRect,"VOLTAR",Color.rgb(42,50,58),17);
        }

        void drawSettings(Canvas c){
            bg(c);float w=getWidth(),h=getHeight();
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("CONFIGURAÇÕES",w/2,65,p);
            p.setTextSize(15);p.setColor(Color.LTGRAY);c.drawText("Dificuldade da IA",w/2,110,p);
            easyRect.set(28,145,w-28,198);normalRect.set(28,215,w-28,268);hardRect.set(28,285,w-28,338);
            button(c,easyRect,"FÁCIL",difficulty==1?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            button(c,normalRect,"NORMAL",difficulty==2?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            button(c,hardRect,"DIFÍCIL",difficulty==3?Color.rgb(35,145,78):Color.rgb(42,50,58),16);
            backRect.set(w/2-100,h-75,w/2+100,h-20);
            button(c,backRect,"VOLTAR",Color.rgb(42,50,58),17);
        }

        void drawGame(Canvas c){
            float w=getWidth(),h=getHeight();
            c.drawColor(Color.rgb(20,105,65));

            // Painel superior
            p.setColor(Color.argb(190,0,0,0));c.drawRect(0,0,w,94,p);
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(22);
            c.drawText("TRUCO ARENA",w/2,28,p);
            p.setTextSize(15);c.drawText("VOCÊ + IA  "+teamAPoints+"  ×  "+teamBPoints+"  RIVAIS",w/2,52,p);
            p.setTextSize(12);p.setColor(Color.rgb(215,230,220));
            c.drawText("Mão "+round+" • Vale "+stake+" • "+tricksA+"×"+tricksB,w/2,75,p);\n\n            // VIRA: carta virada do baralho, sempre visível na mesa.\n            p.setColor(Color.WHITE); p.setTextSize(11);\n            c.drawText("VIRA",w/2,112,p);\n            if(vira!=null) drawCard(c,vira,w/2-32,120,64,88,true);
            p.setColor(Color.rgb(230,240,232)); p.setTextSize(9);
            c.drawText("MANILHA: próxima carta • ♣ > ♥ > ♠ > ♦",w/2,214,p);

            drawOpponent(c,players[2],w/2,132);
            drawTeammate(c,players[1],70,200);
            drawOpponent(c,players[3],w-70,200);

            // Área central: cartas jogadas ficam visíveis até resolver a vaza.
            p.setColor(Color.argb(80,0,0,0));c.drawRoundRect(35,225,w-35,445,24,24,p);
            p.setColor(Color.WHITE);p.setTextSize(12);c.drawText("MESA",w/2,220,p);
            drawPlayedCards(c,w,h);

            p.setTextSize(13);p.setColor(Color.WHITE);
            c.drawText(status,w/2,450,p);

            drawHand(c,w/2,h-172);

            trucoRect.set(w/2-82,h-105,w/2+82,h-55);
            button(c,trucoRect,"TRUCO!",Color.rgb(185,45,45),17);

            if(elevenDecision) draw11(c,w,h);
            if(finished){
                restartRect.set(w/2-110,h-48,w/2+110,h-12);
                button(c,restartRect,"NOVA PARTIDA",Color.rgb(38,52,70),14);
            }
        }

        void drawPlayedCards(Canvas c,float w,float h){
            int n=trick.size();
            if(n==0){
                p.setColor(Color.argb(100,255,255,255));p.setTextSize(14);
                c.drawText("Jogue uma carta",w/2,365,p);
                return;
            }
            for(int i=0;i<n;i++){
                Played x=trick.get(i);
                float cx=w/2+(i-(n-1)/2f)*88;
                float cy=285+(i%2)*28;
                drawCard(c,x.card,cx-32,cy,64,88,true);
                p.setColor(Color.WHITE);p.setTextSize(10);
                c.drawText(x.player==0?"VOCÊ":players[x.player].name,cx,cy+102,p);
            }
        }

        void drawOpponent(Canvas c,Player pl,float x,float y){
            drawAvatar(c,x,y,pl==players[2]?2:3,44);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(11);
            c.drawText(pl.name,x,y+58,p);
            p.setColor(Color.rgb(220,235,225));p.setTextSize(10);
            c.drawText(pl.hand.size()+" cartas",x,y+74,p);
        }

        void drawTeammate(Canvas c,Player pl,float x,float y){
            drawAvatar(c,x,y,1,42);
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

        void draw11(Canvas c,float w,float h){
            p.setColor(Color.argb(245,7,13,10));c.drawRoundRect(15,h/2-100,w-15,h/2+105,22,22,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(22);
            c.drawText("MÃO DE 11",w/2,h/2-57,p);
            p.setTextSize(13);p.setColor(Color.LTGRAY);
            c.drawText("Sua dupla pode jogar valendo 3",w/2,h/2-30,p);
            c.drawText("ou correr e entregar 1 ponto.",w/2,h/2-9,p);
            play11Rect.set(w/2-145,h/2+20,w/2-10,h/2+68);
            run11Rect.set(w/2+10,h/2+20,w/2+145,h/2+68);
            button(c,play11Rect,"VAMOS! 3",Color.rgb(35,135,75),15);
            button(c,run11Rect,"CORRE",Color.rgb(150,45,45),15);
        }

        void drawHand(Canvas c,float x,float y){
            float gap=78;
            float start=x-(players[0].hand.size()-1)*gap/2f-34;
            for(int i=0;i<players[0].hand.size();i++){
                float left=start+i*gap;
                cardRects[i]=new RectF(left,y,left+68,y+94);
                drawCard(c,players[0].hand.get(i),left,y,68,94,false);
            }
            p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(11);
            c.drawText("SUAS CARTAS — toque para jogar",x,y+112,p);
        }

        void drawCard(Canvas c,Card card,float x,float y,float cw,float ch,boolean table){
            // Cartas pretas, com alto contraste e borda clara.
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(9,10,11));
            c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(table?3:2);
            p.setColor(Color.rgb(225,230,225));
            c.drawRoundRect(x,y,x+cw,y+ch,12,12,p);
            p.setStyle(Paint.Style.FILL);

            int textColor=(card.suit.equals("♥")||card.suit.equals("♦"))?Color.rgb(255,95,105):Color.WHITE;
            p.setColor(textColor);p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(table?19:23);c.drawText(card.rank,x+cw/2,y+31,p);
            p.setTextSize(table?24:28);c.drawText(card.suit,x+cw/2,y+66,p);
            p.setTextSize(9);p.setColor(Color.rgb(150,160,155));
            c.drawText("TRUCO",x+cw/2,y+ch-8,p);
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
                if(backRect.contains(x,y)){screen=0;invalidate();return true;}
                return true;
            }

            if(finished&&restartRect.contains(x,y)){startMatch();return true;}
            if(elevenDecision){
                if(play11Rect.contains(x,y))play11();
                else if(run11Rect.contains(x,y))run11();
                return true;
            }
            if(trucoRect.contains(x,y)){askTruco();return true;}
            if(current==0&&!waiting){
                for(int i=0;i<players[0].hand.size();i++){
                    if(cardRects[i]!=null&&cardRects[i].contains(x,y)){playHuman(i);return true;}
                }
            }
            return true;
        }
    }
}
