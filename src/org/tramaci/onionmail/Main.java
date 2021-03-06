/*
 * Copyright (C) 2013-2015  by Tramaci.Org & OnionMail Project
 * This file is part of OnionMail (http://onionmail.info)
 * 
 * OnionMail is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


//XXX Migliorare le eccezioni

package org.tramaci.onionmail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

public class Main {
	public static Config Config = new Config();
	
	public static long VersionID = 0x0001_0008_0000_05FEL;
	public static String Version="1.8.0.1534";
	public static String VersionExtra="";
	public static boolean noTest=false;
	public static SMTPServer[] SMTPS = null;
	public static POP3Server[] POP3S = null;
	public static org.tramaci.onionmail.MailingList.ListThread[] ListThreads = null;
	public static MultiDeliverThread[] MultiTthread = null;
	public static HTTPServer[] HTTP = null;
	private static int mBlackListIPs=0;
	private static int preventTooBlackWrite=0;
	
	public static ExtraThread[] ETH = new ExtraThread[10];
	
	public static ControlService CS = null;
	public static ControlService[] CSP = null; 
	
	public static long[] statusHash = null;
	
	public static volatile long RandLog = 0x4F5450454550544FL;
	
	public static volatile int MaxThread = 0;
	public static volatile int PercThread=0;
	
	public static volatile int statsMaxThread = 0;
	public static volatile int statsPercThread=0;
	
	public static ScheduledExecutorService Kernel = null;
	
	protected static PublicKey FSK = null;
	protected static KeyPair IDK = null;
	protected static SecretKey FSKA = null;
	protected static byte[] FSKIV= null;
	
	protected static DNSCheck DNSCheck = null;
	
	protected static HashMap <String,String> ConfVars=null;
	
	public static String getVersion() { return (Main.Version+" "+Long.toHexString(Main.VersionID)+" "+VersionExtra).trim(); }
	public static boolean NoDelKeys = false;
	
	public static  int Oper = 0;	
	public static final int Oper_Gen_ServerS=1;
	public static final int Oper_Stop=2;
	public static final int Oper_Del_Keys=3;
	public static final int Oper_KCTL = 4;
	
	public static boolean OnlyLoad=false;
	
	public static boolean SelPass=false;
	public static boolean PGPRootMessages=false;
	
	public static String SetPass=null;
	public static boolean CmdRunBoot=false;
	public static boolean CmdDaemon=false;
	public static boolean SetPGPSrvKeys = false;
	
	private static FileWriter out=null;
	private static String OutFile="onionstart.log";
	
	public static String CompiledBy = null;
	public static String ProgPath="./";
	public static String RandomHeart=null;
	public static boolean RSAGenBC = false;
	private static boolean verbose=false;
	private static volatile int SSLETRefreshRateTCR = 0;
	private static volatile int TORCheckPollingRefreshRateTCR=0;
	public static final int STEST_NOP=0;
	public static final int STEST_OK=1;
	public static final int STEST_SMTPE=2;
	public static final int STEST_SOCK=3;
	public static final int STEST_SWAP=4;
	public static final int STEST_POP3ERR=5;
	public static final int STEST_POP3SOCK=6;
	
	public static volatile long cday = 0;
	public static volatile long tcrd = 0;
	
	public static volatile long cmdFlags = 0;
	
	public static final long CF_B_NOBOOTPWL = 1<<11;
	public static final long CF_U_NOSYSOPPWL = 1<<30;
	public static final long CF_BIGPWL=1<<35;
	public static final long CF_R_ROOTPGPPOX=1<<27;
	public static final long CF_W_SSLNOWILC=1<<32;		//NO *.ExitRouteDomain in SSL certificate.
	public static final long CF_C_EXPORTDER=1<<12;			//Export DER SSL certificate.
	public static final long CF_F_NOSKIPFRIEND=1<<15;
	public static final long CF_D_NODERK=1<<13;
	public static final long CF_Z_NOZEROEND=1<<35;
	public static final long CF_X_FASTEXIT=1<<33;
	
	
	public static volatile Furamide furamidal = null;
	
	public static long getUsedMememory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}
	
	public int[] SelfTest(boolean term,boolean out) throws Exception {
		int cx= SMTPS.length;
		if (out && !Config.LogStdout) Main.echo("Self server test:\n"); else Config.GlobalLog(Config.GLOG_All, "MAIN", "Start test servers");
		
		Socket	RS=null;
		OutputStream RO=null;
		BufferedReader RI=null;
		SMTPReply Re=null;
		int fun=0;
		int[] Rs = new int[cx];
		
		for (int ax=0;ax<cx;ax++) {
			if (out && !Config.LogStdout) Main.echo("\tTest: "+J.Spaced(SMTPS[ax].Identity.Nick, 25)+"... SMTP "); else Config.GlobalLog(Config.GLOG_All,SMTPS[ax].Identity.Nick, "SMTP Test start");
				try {
					RS=null;
					RS = J.IncapsulateSOCKS(Config.TorIP, Config.TorPort, SMTPS[ax].Identity.Onion,25);
					RO = RS.getOutputStream();
					RI  =J.getLineReader(RS.getInputStream());
					RS.setSoTimeout(Config.MaxSMTPSessionInitTTL);
					Re = new SMTPReply(RI);
					if (Re.Code<200 || Re.Code>299) throw new Exception("@"+Re.toString().trim()); 
					Re = SrvSMTPSession.RemoteCmd(RO,RI,"EHLO iam.onion");
					if (Re.Code<200 || Re.Code>299) throw new Exception("@"+Re.toString().trim());
					if (!Re.Msg[0].toLowerCase().contains(SMTPS[ax].Identity.Onion)) throw new PException("Fatal error: The server at `"+J.IP2String(SMTPS[ax].Identity.LocalIP)+":25` is not `"+SMTPS[ax].Identity.Nick+"`");
					try { Re = SrvSMTPSession.RemoteCmd(RO,RI,"QUIT"); } catch(Exception I) {}
					try { if (RS!=null) RS.close(); } catch(Exception I) {}
					try { if (RO!=null) RO.close(); } catch(Exception I) {}
					try { if (RI!=null) RI.close(); } catch(Exception I) {}
					Rs[ax] = Main.STEST_OK;
					Config.GlobalLog(Config.GLOG_All,SMTPS[ax].Identity.Nick, "SMTP Test Ok");
					} catch(Exception E) {
						try { if (RS!=null) RS.close(); } catch(Exception I) {}
						try { if (RO!=null) RO.close(); } catch(Exception I) {}
						try { if (RI!=null) RI.close(); } catch(Exception I) {}
						String mx = E.getMessage();
						if (mx.startsWith("@")) {
								mx=mx.substring(1);
								if (out) Main.echo("Error: "+mx+"\n");
								Config.GlobalLog(Config.GLOG_Server, SMTPS[ax].Identity.Nick, "Server SMTP test error: "+mx);
								Rs[ax] = Main.STEST_SMTPE;
								} else {
								if (E instanceof PException) {
									if (out) Main.echo(E.getMessage());
									Config.GlobalLog(Config.GLOG_Bad | Config.GLOG_All | Config.GLOG_Server, "MAIN", E.getMessage());
									Rs[ax]=Main.STEST_SWAP;
									if (term) endProc(0);
									}  
								if (out) Main.echo("Exception: "+mx+"\n");
								Config.EXC(E, "TEST `"+ SMTPS[ax].Identity.Nick+"`");
								Rs[ax] = Main.STEST_SOCK;
								} 
					}
				
				if (out && !Config.LogStdout) Main.echo("POP3 "); else  Config.GlobalLog(Config.GLOG_All,SMTPS[ax].Identity.Nick, "POP3 Test start");
				
				try {
					RS=null;
					RS = J.IncapsulateSOCKS(Config.TorIP, Config.TorPort, SMTPS[ax].Identity.Onion,110);
					RO = RS.getOutputStream();
					RI  =J.getLineReader(RS.getInputStream());
					RS.setSoTimeout(Config.MaxSMTPSessionInitTTL);
					String in = RI.readLine();
					if (!in.toLowerCase().contains(SMTPS[ax].Identity.Onion)) throw new PException("Fatal error: POP3 The server at `"+J.IP2String(SMTPS[ax].Identity.LocalIP)+":"+SMTPS[ax].Identity.LocalPOP3Port+"` is not `"+SMTPS[ax].Identity.Nick+"`");
				
					try { 
						RO.write("QUIT\r\n".getBytes());
						in = RI.readLine();
						} catch(Exception I) {}
					
					try { if (RS!=null) RS.close(); } catch(Exception I) {}
					try { if (RO!=null) RO.close(); } catch(Exception I) {}
					try { if (RI!=null) RI.close(); } catch(Exception I) {}
					Config.GlobalLog(Config.GLOG_All,SMTPS[ax].Identity.Nick, "POP3 Test Ok");
					} catch(Exception E) {
						try { if (RS!=null) RS.close(); } catch(Exception I) {}
						try { if (RO!=null) RO.close(); } catch(Exception I) {}
						try { if (RI!=null) RI.close(); } catch(Exception I) {}
						String mx = E.getMessage();
						if (mx.startsWith("@")) {
								mx=mx.substring(1);
								if (out) Main.echo("Error: "+mx+"\n");
								Config.GlobalLog(Config.GLOG_Server, SMTPS[ax].Identity.Nick, "Server POP3 test error: "+mx);
								Rs[ax] = Main.STEST_POP3ERR;
								} else {
								if (out) Main.echo("Exception: "+mx+"\n");
								Config.EXC(E, "TEST `"+ SMTPS[ax].Identity.Nick+"`");
								if (E instanceof PException) {
									Rs[ax] = Main.STEST_SWAP;
									if (term) endProc(0);
									} else Rs[ax]=Main.STEST_SOCK;
								} 
					}
				
				if (out && !Config.LogStdout) Main.echo("Ok\n");
				fun++;
			}
		
		if (out && !Config.LogStdout) Main.echo("\nTest complete: "+cx+" Servers "+(cx-fun)+" Errors "+fun+" Ok\n\n");
		if (cx==fun) Config.GlobalLog(Config.GLOG_All, "MAIN.TEST", "OnionMail "+fun+" servers, running Ok");
		return Rs;
		}
	
	private static void RedirectOut() throws Exception {
		try {
			out = new FileWriter(Main.OutFile, false);
		} catch(Exception E) {
			out=null;
			out("Can't daemonize\n");
		}
	}
	
	private boolean TermCmd()   {
		boolean stat1=false;
		try {
			echo("Closing previous instances:\t");
			Socket s = new Socket(Config.ControlIP,Config.ControlPort);
			s.setSoTimeout(5000);
			BufferedReader i = J.getLineReader(s.getInputStream());
			OutputStream o = s.getOutputStream();
			String Rns="";
			
			String t0 = i.readLine();
			if (t0==null || t0.length()==0) {
				echo("Error 1\n");
				try { s.close(); } catch(Exception I) {}
				return true;
				}
			stat1=true;	
			
			if (t0.indexOf('+')!=0) {
				echo("Error 2: `"+t0.trim()+"`\n");
				try { s.close(); } catch(Exception I) {}
				return true;
				} else {
					int a= t0.indexOf('<');
					int b= t0.indexOf('>');
					if (b>a) Rns = t0.substring(a+1, b);
				}
			
			o.write(("sux "+Stdio.Dump(Stdio.md5a(new byte[][] { Rns.getBytes() ,Config.RootPass.getBytes()}))+"\r\n").getBytes());
			t0 = i.readLine();
			if (t0==null || t0.length()==0) {
					echo("Error 3\n");
					try { s.close(); } catch(Exception I) {}
					return true;
					}
			
			if (t0.indexOf('+')!=0) {
					echo("Error 4: `"+t0.trim()+"`\n");
					try { s.close(); } catch(Exception I) {}
					return true;
				}
			
			o.write("stop now\r\n".getBytes());
			t0 = i.readLine();
			if (t0==null || t0.length()==0) {
					echo("Error 5\n");
					try { s.close(); } catch(Exception I) {}
					return true;
					}
			
			if (t0.indexOf('+')!=0) {
					echo("Error 6: `"+t0.trim()+"`\n");
					try { s.close(); } catch(Exception I) {}
					return true;
				}
			try { s.close(); } catch(Exception I) {}
			echo("Ok\n");
		} catch(Exception E) {
			if (stat1) {
				echo("Error: `"+E.getMessage()+"`\n"); 
				return true;
			} else {
				echo("Ok\n");
				return false;
				}
			
		}
		return false;
	}
		
	@SuppressWarnings("static-access")
	public void Start(String fc) throws Exception {
		int numSrv=0;
		int mht=0;
		try {
			
			Config = Config.LoadFromFile(fc);
			if (Oper==Main.Oper_KCTL) {
				echo("Remote KCTL operation\nPaste here the KCTL Sequence:\n");
				String kctl = J.ASCIISequenceReadI(System.in, Const.ASC_KB_KCTL);
				echo("\nEnter Onion address: ");
				BufferedReader br = J.getLineReader(System.in);
				String oni;
				while(true) {
					echo(">");
					oni= br.readLine();
					if (oni==null) System.exit(2);
					oni=oni.toLowerCase().trim();
					if (oni.matches("[a-z0-9]{16}\\.onion")) break;
					}
				echo("Enter password: ");
				String pwl = br.readLine();
				if (pwl==null) System.exit(2);
				pwl=pwl.trim();
				if (pwl.length()<4) System.exit(2);
				Config.LogStdout=true;
				SrvIdentity S = new SrvIdentity(Config);
				S.Onion = oni.toLowerCase().trim();
				
				while(true) {
					echo("Remote DERK options for `"+oni+"`\n");
					echo("Choose action:\n\t(0) Quit\n\t(1) Unlock/Reset Credit\n\t(2) Lock\n\t(3) Destroy\n\t>");
					int act = Config.parseInt(br.readLine().trim());
					if (act==0 || act> 3) System.exit(0);
					String acts="";
					if (act==1) acts="start";
					if (act==2) acts="set 0";
					if (act==3) acts="del";
					echo("Running remote KCTL\n");
					RemoteKSeedInfo[] xy = S.RemoteDoKCTLAction(acts,kctl ,oni,pwl);
					int cx= xy.length;
					for (int ax=0;ax<cx;ax++) {
						echo(J.Spaced(Integer.toString(cx), 4)+J.Spaced(xy[ax].Onion, 24)+J.Spaced(xy[ax].Ok ? "Ok":"Error", 6)+xy[ax].Confirm+"\n");
						}
					}
				
				// 
			}
			
			if (Oper==Main.Oper_Del_Keys) {
					DelKeys();
					endProc(0);
				}
			
			if(Oper==Main.Oper_Stop) {
					TermCmd();
					endProc(0);
				}
			
			} catch(Exception E) {
			String st = E.getMessage()+"";
			if (st.startsWith("@")) {
			echo(st.substring(1)+"\n");	
			} else echo("Config error "+E.getMessage()+"\n");
			
			//E.printStackTrace();
			Main.endProc(2);
			}
		
		if (Oper==Main.Oper_Gen_ServerS) { 
				if (Main.ConfVars!=null) out("OM:[COMPLETE] ");
				echo("\nOperation complete!\n");
				Main.endProc(0);
				}	
		
		if (Oper!=Main.Oper_Stop && TermCmd()) {
			echo("Error: The control port is used, some TCP ports maybe in use!\n");
			Main.endProc(2);
			}
		
		if (!J.TCPRest(Config.TorIP, Config.TorPort)) {
			if (Main.ConfVars!=null) out("OM:[ERROR] ");
			echo("\nCan't connect to TOR via `"+J.IP2String(Config.TorIP)+":"+Integer.toString(Config.TorPort)+"`\n");
			Main.endProc(2);
			}
			
		ListThreads= new org.tramaci.onionmail.MailingList.ListThread[Config.ListThreadsMax];
		MultiTthread = new MultiDeliverThread[Config.ListThreadsMax];
		
		if (Config.SMPTServer.length==0) echo("Warning:\n\tNo SMTP Server defined!\n\n");
		try {
			
			echo("Start DNSCheck: ");
			DNSCheck = new DNSCheck(Config);
			echo("Ok\n");
						
			echo("Running SMTP Server:\n");
			int cx = Config.SMPTServer.length;
			
			SMTPS = new SMTPServer[cx*3];
			POP3S = new POP3Server[cx];
			
			String otsm="\n";
			
			for (int ax=0;ax<cx;ax++) otsm+=Config.SMPTServer[ax].Onion.trim().toLowerCase()+"\n";
			for (int ax=0;ax<cx;ax++) Config.SMPTServer[ax].OnTheSameMachine = otsm;
			
			int bx=0;
			
			for (int ax=0;ax<cx;ax++) {
				echo("\nStart "+J.Limited("`"+Config.SMPTServer[ax].Nick+"`",40)+"\n");
				echo("\tOnion:\t"+Config.SMPTServer[ax].Onion+"\n");
						
				echo("\tExit:  \t");
				if (Config.SMPTServer[ax].EnterRoute) echo("YES!\n\tQFDN:\t"+Config.SMPTServer[ax].ExitRouteDomain+"\n"); else echo("No\n");
								
				try {
					echo("\tSMTP: \t"+J.Spaced(J.IP2String(Config.SMPTServer[ax].LocalIP)+":"+Config.SMPTServer[ax].LocalPort,25));
					SMTPS[bx] = new SMTPServer(Config,Config.SMPTServer[ax],SMTPServer.SM_TorServer);
					echo("\ttor\tOk\n");
					bx++;
					if (Config.SMPTServer[ax].EnterRoute) {
						String x = Config.SMPTServer[ax].ExitIP==null ? "0.0.0.0" : J.IP2String(Config.SMPTServer[ax].ExitIP);
						echo("\tSMTP: \t"+J.Spaced(x+":"+Config.SMPTServer[ax].LocalPort,25));
						SMTPS[bx] = new SMTPServer(Config,Config.SMPTServer[ax],SMTPServer.SM_InetServer);
						echo("\tinet\tOk\n");
						bx++;
						echo("\tSMTP: \t"+J.Spaced(x+":"+Config.SMPTServer[ax].ExitAltPort,25));
						SMTPS[bx] = new SMTPServer(Config,Config.SMPTServer[ax],SMTPServer.SM_InetAlt);
						echo("\tialt\tOk\n");
						
						bx++;
						}
					
					} catch(Exception E) {
					echo("!Error\n\t"+E.getMessage()+"\n");
					if (Config.Debug) Config.EXC(E, "SMTP."+Config.SMPTServer[ax].Nick);
					Main.endProc(2);
					}
				
				if ((Main.cmdFlags & Main.CF_C_EXPORTDER)!=0) {
						int ecx = Config.SMPTServer.length;
						for (int eax=0;eax<ecx;eax++) try {
							String fo = Config.SMPTServer[eax].Maildir+"/certificate.der";
							if (new File(fo).exists()) continue;
							byte[] der =  Config.SMPTServer[eax].MyCert.getEncoded();
							Stdio.file_put_bytes(fo, der);
							der=null;
							} catch(Exception E) { Main.echo("ExportDer: "+E.getMessage()+"\n"); }
					}
				
				try {
					echo("\tPOP3:\t"+J.Spaced(J.IP2String(Config.SMPTServer[ax].LocalIP)+":"+Config.SMPTServer[ax].LocalPOP3Port,25));
					POP3S[ax] = new POP3Server(Config,Config.SMPTServer[ax]);
					echo("\tOK\n");
					if (Config.SMPTServer[ax].HasHTTP) mht++;
					} catch(Exception E) {
					echo("!Error\n\t"+E.getMessage()+"\n");
					if (Config.Debug) Config.EXC(E, "POP3."+Config.SMPTServer[ax].Nick);
					Main.endProc(2);
					}
				
				}
			numSrv=bx;
			//echo("\n"); ???
			} catch(BindException BE) {
				echo("\nAddress in use!\n");
				Main.endProc(2);
			}
		
		HTTP = new HTTPServer[mht+1];
		int bx=0;
		for (int ax=0;ax<Config.SMPTServer.length;ax++) {
			if (!Config.SMPTServer[ax].HasHTTP) continue;
			echo("HTTP Server: \t"+Config.SMPTServer[ax].Nick+"\t"+J.IP2String(Config.SMPTServer[ax].LocalIP)+":"+Config.SMPTServer[ax].LocalHTTPPort+"\t");
			HTTP[bx] = new HTTPServer(Config,Config.SMPTServer[ax]);
			echo("Ok\n");
			bx++;
			}
			
		SMTPServer[] sa = new SMTPServer[numSrv];
		System.arraycopy(SMTPS, 0, sa, 0,  numSrv);
		SMTPS=sa;
		
		Main.RandLog = Stdio.NewRndLong();
		
		echo("Control port:\t"+J.IP2String(Config.ControlIP)+":"+Config.ControlPort+"\t");
		try {
			CS = new ControlService(Config,SMTPS);
			echo("Ok\n");
			
			int cx = Config.SMPTServer.length;
			int dx=0;
			String t0="\n";
						
			for (int ax=0;ax<cx;ax++) if (Config.SMPTServer[ax].PublicControlIP!=null) dx++;
			Main.CSP = new ControlService[dx];
			
			for (int ax=0;ax<cx;ax++) { 
				if (Config.SMPTServer[ax].PublicControlIP!=null) {
					String t1=J.IP2String(Config.SMPTServer[ax].PublicControlIP)+":"+Config.SMPTServer[ax].PublicControlPort;
					echo("Public control port:\t"+J.Spaced(Config.SMPTServer[ax].Nick, 32));
					echo(J.Spaced(J.IP2String(Config.SMPTServer[ax].PublicControlIP)+":"+Config.SMPTServer[ax].PublicControlPort,25));
					
					if (t0.contains("\n"+t1+"\n")) {
						echo("Error!\n\tIP+Port is in use by another control port!\n");
						Main.endProc(2);
						}
					
					Main.CSP[ax] = new ControlService(Config ,Config.SMPTServer[ax],Config.SMPTServer[ax].PublicControlPort,Config.SMPTServer[ax].PublicControlIP);
					echo("Ok\n");
					}
				}
			} catch(Exception BE) {
				echo("Error "+BE.getMessage()+"\n");
				Main.endProc(2);
			}
		
		echo("Service Started\n");
				
	if (Main.NoDelKeys) echo("Warning: NoDelKeys ENABLED!\n");  else DelKeys();
			
	if (Main.SetPass!=null) for (int ax=0;ax<5;ax++) {
		Main.SetPass = J.RandomString(16);
		Main.SetPass = null;
		System.gc();
		}
	
		if (Config.LogFile==null) echo("\nLog to STDOUT:\n"); else {
						if (Config.RLOG!=null) echo("LogFile is in RSA mode!\n");  else echo("LogFile is in plain text mode!\n");
						}
	
	Config.GlobalLog(Config.GLOG_All, "MAIN", "OnionMail is running!");
	
	if (Main.ConfVars!=null) {
		boolean ac= Main.ConfVars.containsKey("global-autoclose");
		
		if (Main.ConfVars.containsKey("global-autodelete")) {
			for (String k:Main.ConfVars.keySet()) {
				Main.ConfVars.put(k, "");
				Main.ConfVars.remove(k);
				}
			System.gc();
			Main.ConfVars=null;
			System.gc();
			Main.out("OM:[DELETE]\n");
			}
		Main.out("OM:[COMPLETE]\n");
		if (ac) Main.endProc(0);
		Main.out("OM:[RUNNING]\n");
		}
	
	Thread.sleep(2000);		
	int pause = (int) (System.currentTimeMillis()/1000L)+15*60;
	SSLETRefreshRateTCR = pause;
	TORCheckPollingRefreshRateTCR=pause;
	
	if (!Main.noTest) SelfTest(true,true);
	if (Config.UseKernel) startKernel(); 
	}
	
	private void DelKeys() throws Exception {
		
			String s0 ="";
			int cx = Main.SMTPS.length;
			for (int ax=0;ax<cx;ax++) {
				String s1;
				if (Config.MindlessCompilant) {
					s1 =SMTPS[ax].Identity.Maildir+"/keyblock.txt";
					if (new File(s1).exists()) s0+=s1+"\n";
					}
				s1=SMTPS[ax].Identity.Maildir+"/sysop.txt";
				if (new File(s1).exists()) s0+=s1+"\n";
				}
			s0=s0.trim();
			if (s0.length()==0) return;
			
			String fco[] = s0.split("\\n+");
			cx = fco.length;
			if (cx>0) {
				echo("\nWarning:\n\t");
				echo("Some reserved files are detected!\n\tThese files contain the keys and must be removed with a wipe.\n");
				echo("Do you want to remove these files now?\n");
				for (int ax=0;ax<cx;ax++) echo("\t"+fco[ax]+"\n");
				echo(" Yes/No ? ");
				while(true) {
					int ax = System.in.read();
					if (ax==0x4e || ax==0x6e) return;
					if (ax==0x59 || ax==0x79) break;
				}
				echo("\n");
				for (int ax=0;ax<cx;ax++) {
					echo("\t Wipe: `"+fco[ax]+"`\t... ");
					try { J.Wipe(fco[ax], false); } catch(Exception X) { echo("Error: "+X.getMessage()+" "); }
					if (new File(fco[ax]).exists()) echo("Can't delete!\n"); else echo("Ok\n");
					}
			echo("\n");
			}
	}

public static void main(String args[]) {
		Main N=null;
					
		try {
			LibSTLS.AddBCProv();
			CompiledBy = J.Compiler();
		
			File X = new File(".");
			ProgPath = X.getAbsolutePath().toString();
			ProgPath=ProgPath.replace("\\", "/");
			if (ProgPath.endsWith("/.")) ProgPath=ProgPath.substring(0,ProgPath.length()-1);
			if (!ProgPath.endsWith("/")) ProgPath+="/"; //Non avver� mai!
			
			String fc=null;
			for(String x:new String[] { "etc/config.conf", "onionmail.conf" , "/etc/onionmail/config.conf"} ) if (new File(x).exists()) fc = x;
												
			int cx = args.length;
			boolean fp=true;
			
			for (int ax=0;ax<cx;ax++) {
				String cmd = args[ax].toLowerCase().trim();		
					
					if (cmd.compareTo("-d")==0) {
						CmdDaemon=true;
						RedirectOut();
						}
					
					if (cmd.compareTo("-dr")==0) {
					if ((ax+1)>=cx) {
							echo("Error in command line: -dr\n\tFile required!\n");
							Helpex();
							return;
							}
		
					Main.OutFile = args[ax+1].trim();
					ax++;
					RedirectOut();
					}
				
					if (cmd.compareTo("-q")==0) fp=false;
					
				}
						
			if (fp) echo("\nOnionMail Ver. "+Main.getVersion()+"\n\t(C) 2013-2014 by Tramaci.org\n\t(C) 2013-2015 by OnionMail Project\n\t(C) 2013-2015 by mes3hacklab\n\tSome rights reserved.\n\n");
			
			for (int ax=0;ax<cx;ax++) {
				boolean fm=false;
				String cmd = args[ax].trim();		
				
				if (cmd.startsWith("-F:")) {
					fm=true;
					int i = cmd.indexOf(':');
					String st=cmd.substring(i+1);
					st=st.toLowerCase();
					st=st.trim();
					i = st.length();
					
					for (int j = 0; j < i; j++) {
						String fl = st.substring(j,j+1);
						try {
							int k = Integer.parseInt(fl,36);
							Main.cmdFlags |= 1<<k;
							} catch(Exception E) {
								Main.echo("Invalid flag: `"+fl+"`\n");
								Main.Helpex();
								return;
							}
						}
					
					}
				
				if (cmd.compareTo("-d")==0) fm=true;
				
				if (cmd.compareTo("-dr")==0) {
					fm=true;
					ax++;
					}
				
				if (cmd.compareTo("-q")==0) fm=true; 
								
				if (cmd.compareTo("-f")==0) { 
						fm=true;
						if ((ax+1)>=cx) {
							echo("Error in command line: -f\n\tFile required!\n");
							Helpex();
							return;
							}
						fc = args[ax+1];
						if (!new File(fc).exists()) {
							echo("\nCan't open `"+fc+"`\n");
							Main.endProc(2);
							}
						ax++;
						}
				
				if (cmd.compareTo("-ntx")==0) {
					noTest=true;
					fm=true;
				}
				
				if (cmd.compareTo("-rc")==0  && (ax+1)<args.length) {
					ax++;
					String x = args[ax];
					
					try {
							byte[] ff0;
							if (new File(x).exists()) ff0= Stdio.file_get_bytes(x); else ff0=new byte[] { 48 };
							int ff1 = Integer.parseInt(new String(ff0).trim());
							ff1++;
							ff0 = Integer.toString(ff1).getBytes();
							Stdio.file_put_bytes(x,ff0);
						} catch(Exception I) { Main.echo("-RC: Counter error `"+x+"' "+I.getMessage()+"\n"); }
				
					fm=true;
				}
				
				if (cmd.compareTo("--srv-passwd")==0 && ax+1<args.length) {
				
					int j = args.length;
					boolean ba=false;
					String curp=null;
					for (int i=0;i<j;i++) {
						if (args[i].compareTo("-bm")==0) ba=true;
						if (args[i].compareTo("-bf")==0 && i+1<j) curp=args[i+1];
					}
					try {
						srvPasswd(args[ax+1],curp,ba);
					} catch(Exception e) {
						if (ba) echo("OM:[ERR]\n"); else echo("Error: "+e.getMessage()+"\n");
					}
					Main.endProc(0);
				}
				
				if (cmd.compareTo("--rnd-passwd")==0 && ax+2<args.length) {
					int lp = J.parseInt(args[ax+1]);
					int sc =J.parseInt(args[ax+2]);
					if (lp>0 && sc>0) Main.echo( (fp ? "Password:" : "") +J.GenPassword(lp, sc)+"\n");
					System.exit(0);	
					}
				
				if (cmd.compareTo("--rnd-passcr")==0 && ax+2<args.length) {
					int lp = J.parseInt(args[ax+1]);
					int sc =J.parseInt(args[ax+2]);
					if (lp>0 && sc>0) {
							String p =J.GenPassword(lp, sc);
							Main.echo( (fp ? "Password: " : "") +p+"\n");
							Main.echo( (fp ? "Crypt: " : "")+ J.GenCryptPass(p)+"\n");
							}
					System.exit(0);	
					}
				
				if (cmd.compareTo("--rnd-epass")==0 && ax+1<args.length) {
					Main.echo(J.GenEPassword(J.parseInt(args[ax+1]))+"\n");
					System.exit(0);
				}
				
				if (cmd.compareTo("--rnd-passcrn")==0 && ax+3<args.length) {
					int cl = J.parseInt(args[ax+1]);
					int lp = J.parseInt(args[ax+2]);
					int sc =J.parseInt(args[ax+3]);
					int[] pt=null;
					boolean[] isEkey=null;
					if (args[ax+1].contains("{")) {
						String tmp="";
						for (int al=ax+2;al<cx;al++) {
							String t0 = args[al].replace('{', ' ');
							t0 = t0.replace('}', ' ');
							t0 = t0.replace('\n', ' ');
							tmp=tmp+t0.trim()+"\n";
							if (args[al].contains("}")) break;
							}
						String[] tmp2= tmp.split("\\n+");
						cl = tmp2.length;
						pt =new int[cl];
						isEkey = new boolean[cl];
						for (int al =0;al<cl;al++) {
								String t0 = tmp2[al];
								isEkey[al] = t0.contains("E");
								isEkey[al] = t0.contains("e");
								t0=t0.replace('E', ' ');
								t0=t0.replace('e', ' ');
								t0=t0.trim();
								pt[al] = J.parseInt(t0);
								}	
						}
					
					for (int al=0;al<cl;al++) {
						long t = System.currentTimeMillis();
						t+=100;
						t+=127&Stdio.NewRndLong();
						while(System.currentTimeMillis()<t) Stdio.NewRndLong();
						if (pt!=null) lp=sc=pt[al];
						boolean ek = isEkey==null ? false : isEkey[al];
						String p = ek ? J.GenEPassword(lp) : J.GenPassword(lp, sc);
						String n = Integer.toString(al+1);
						Main.echo(
									"P"+n+": "+p+"\n"+
									"S"+n+": "+J.GenCryptPass(p)+"\n"+
									"T"+n+": " + (ek ? 'E' : 'N')+"\n")
									;
						}
					System.exit(0);	
					}
				
				if (cmd.compareTo("-bm")==0) {
					BatchMode();
					fm=true;
					}
				
				if (cmd.compareTo("-v")==0) { 
						fm=true;
						verbose=true;
						}
				
				if (cmd.compareTo("--stop")==0) { 
						Oper=Oper_Stop; 
						fm=true; 
						OnlyLoad=true; 
						}
							
				if (cmd.compareTo("--gen-passwd")==0) { 
						GenPassword(fp); 
						return; 
						}
				
				if (cmd.compareTo("--gen-servers")==0) { 
						Oper=Oper_Gen_ServerS; 
						fm=true; 
						}
				
				if (cmd.compareTo("--del-keys")==0) { 
						OnlyLoad=true;
						Oper=Oper_Del_Keys; 
						fm=true; 
						}
								
				if (cmd.compareTo("--pgp")==0) { 
						PGPRootMessages=true;
						fm=true; 
						}
				
				if (cmd.compareTo("--kctl")==0) { 
						OnlyLoad=true;
						Oper=Oper_KCTL; 
						fm=true; 
						}
				
				if (cmd.compareTo("--reboot")==0) {
					fm=true;
					CmdRunBoot=true;
					}
				
				if (cmd.compareTo("--test-java")==0) {
					boolean x = LibSTLS.TestJavaDiMerdaBug(true);
					try {
						String s= x ? "yes":"no";
						Stdio.file_put_bytes(fc+".sslTest", s.getBytes());
						} catch(Exception I) {}
					System.exit(x ? 1 : 0);
					}
				
				if (cmd.compareTo("--test-java-b")==0) {
					boolean x = LibSTLS.TestJavaDiMerdaBug(false);
					out("OM:[TEST]" + (x ? "BAD":"GOOD")+"\n");
					try {
						String s= x ? "yes":"no";
						Stdio.file_put_bytes(fc+".sslTest", s.getBytes());
						} catch(Exception I) {}
					System.exit(x ? 1 : 0);
					}
												
				if (cmd.compareTo("--gen-log")==0 && (ax+1)<args.length) {
					ax++;
					fm=true;
					String lf = args[ax];
					Main.echo("\nRSA Log generator.\n\tWrite Passphrase:> ");
					BufferedReader In = J.getLineReader(System.in);
					String pw = In.readLine();
					pw=pw.trim();
					Main.echo("\rRead Passphrase:> ");
					String pr = In.readLine();
					pr=pr.trim();
					RSALog.logFileCreate(lf, pw.getBytes(),pr.getBytes(), 1000);
					System.exit(0);
					}
				
				if (cmd.compareTo("--read-log")==0 && (ax+1)<args.length) {
					ax++;
					fm=true;
					String lf = args[ax];
					if (SetPass==null) {
						Main.echo("\nRSA Log Reader.\n\tRead Passphrase:> ");
						BufferedReader In = J.getLineReader(System.in);
						SetPass = In.readLine();
						SetPass=SetPass.trim();
						}
					
					RSALog rl = new RSALog(lf,SetPass.getBytes(),true);
					while(rl.feof()) {
						RSALog.LogData ld = rl.read();
						if (ld==null) continue;
						Main.echo(ld.toString()+"\n");
						}
					
					System.exit(0);
					}
							
				if (cmd.compareTo("-sp")==0) { 
						SelPass=true; 
						fm=true; 
						}
				
				if (cmd.compareTo("--set-pgp")==0) {
					fm=true;
					SetPGPSrvKeys=true;
				}
								
				if (cmd.compareTo("-ndk")==0) { 
						NoDelKeys=true; 
						fm=true;
						}
				
				if (cmd.compareTo("-p")==0 && (ax+1)<args.length) {
						ax++;
						if (SetPass!=null) 
							SetPass = J.by2pass(J.Der2048(SetPass.getBytes(), args[ax].getBytes()));
							else 
							SetPass = args[ax];
						fm=true;
						}
				
				if (cmd.compareTo("-pi")==0) {
						ax++;
						out("OM:[PASS] Send password to STDIN\n");
						BufferedReader In = J.getLineReader(System.in);
						String pw = In.readLine();
						pw=pw.trim();
						
						if (SetPass!=null) 
							SetPass = J.by2pass(J.Der2048(SetPass.getBytes(), pw.getBytes()));
							else 
							SetPass =pw;
						fm=true;
						pw=null;
						}
				
				if (cmd.compareTo("-rpf")==0 && (ax+1)<args.length) {
					String rpf = args[ax+1];
					fm=true;
					ax++;
					try {
						Config G = new Config();
						G.DefaultPort=8000;
						G.Debug = verbose;	
						String pw = getRemotePassphrase(rpf,G);	
						if (pw==null) throw new Exception("@No passphrase found!");
						if (SetPass!=null) 
								SetPass = J.by2pass(J.Der2048(SetPass.getBytes(), pw.getBytes()));
								else 
								SetPass =pw;
						pw=null;
						System.gc();
						} catch(Exception E) { EXCM(E); }
					}
				
				if (cmd.compareTo("-pf")==0 && (ax+1)<args.length) {
						ax++;
						String fpa = args[ax];
						fm=true;
						if (!new File(fpa).exists()) {
							echo("\nError: No password file: `"+fpa+"`.\n");
							System.exit(2);
							}
						try {
								SetPass=Main.DerKeyfile(fpa, SetPass);
							} catch(Exception E) {
								echo("Error: `"+E.getMessage()+"`\n");
								Main.endProc(2);
							}
						}
				
				if (cmd.compareTo("--show-passwd")==0) {
					if (SetPass==null) echo("No password!\n"); else echo(SetPass+"\n");
					System.exit(0);
					}
				
				if (cmd.compareTo("--gen-keyfile")==0 && (ax+2)<args.length) {
					try {
						Main.GenKeyFile(args[ax+1], J.parseInt(args[ax+2]));
						} catch(Exception E) {
						echo("Error: `"+E.getMessage()+"`\n");
						System.exit(2);
						}
					Main.endProc(0);
					}
										
				if (cmd.compareTo("-?")==0) { 
						Helpex();  
						return; 
						}
				
				if (cmd.compareTo("--gen-rpass")==0) {
					try {
						starterCreation();
						Main.endProc(0);
						} catch(Exception E) { EXCM(E); }
					fm=true;
					}
				
				if (cmd.compareTo("--rpass-server")==0 && (ax+2)<args.length) try {
					fm=true;
					Config G = new Config();
					G.DefaultPort=8000;
					G.Debug = verbose;
					InetAddress Lip = G.ParseIp(args[ax+1]);
					beginPassphraseServer(Lip,args[ax+2],G);
					Main.endProc(0);
					} catch(Exception E) { EXCM(E); }
								
				if (!fm) {
					echo("Invalid command line parameter `"+cmd+"`\n");
					Helpex(); 
					return;
					}
				
				}
			
			if (fc==null) {
				echo("\nCan't find any config file!\n");
				System.exit(2);
				}
			
			echo("Load Config '"+fc+"'\n");
			N = new Main();
			if (verbose) {
					N.Config.Debug=true;
					N.Config.DNSLogQuery=true;
					N.Config.LogStdout=true;
					}
			N.Start(fc);
			if (N.Config==null) { 
				echo("\nCan't start!\n");
				} 
		} catch(Exception E) { 
			if (N!=null && N.Config!=null) { 
				if (N.Config.Debug) EXC(E,"Main");
				} else EXC(E,"Main");
			echo("Fatal Error: "+E.getMessage()+"\n");
			if (Main.ConfVars!=null) Main.out("OM:[ERROR] "+E.getMessage()+"\n");
			}
      }

	private static void GenPassword(boolean fp) throws Exception {
		if (fp) echo("\nPassword generator tool\nEnter password (UTF-8 encoding):\n");
		BufferedReader i =J.getLineReader(System.in);
		String p =i.readLine();
		p=p.trim();
		String q = J.GenCryptPass(p);
		echo("\n"+q+"\n");
		
	}

	private static String DerKeyfile(String fpa,String altpass) throws Exception {
			if (new File(fpa).length()>65536) throw new Exception("@Keyfile too big `"+fpa+"`");
		
			byte[] b = Stdio.file_get_bytes(fpa);
			byte[] c;
			
			if (altpass==null) 
					c=J.Der2048(fpa.getBytes(), ("OnionMail.VirtualClass.getDefaultKPSSWD(`"+J.md2st(Stdio.md5(fpa.getBytes()))+"`)").getBytes());
					else
					c=altpass.getBytes();
			
			b= J.Der2048(b, c);
			return J.by2pass(b);
		}
		
	public static void GenKeyFile(String fpa,int size) throws Exception {
		if (size>65535) size=65535;
		if ((size&7)!=0) size++;
		size>>=3;
		if (size<32) size=32;
		echo("Generating global salt keyfile `"+fpa+"`\n\tbits=`"+(size*8)+"`\n");
		byte[] b = new byte[size];
		Stdio.NewRnd(b);
		Stdio.file_put_bytes(fpa, b);
	
	}

	private static void Helpex() {
		
		InputStream i = Main.class.getResourceAsStream("/resources/help");
		BufferedReader h = J.getLineReader(i);
		
		while(true) try {
			String li=h.readLine();
			if (li==null) break;
			echo(li+"\n");
			} catch(Exception E) { E.printStackTrace(); break; }
		
		try {		h.close(); } catch(Exception I) {}
		try {		i.close(); } catch(Exception I) {}
		
		}	 

	public static void echo(String st) {
		if (out!=null) try {
				synchronized (out) {
					out.write(st);
					out.flush();
					}
				} catch (Exception E) {System.out.print(st); } else System.out.print(st);
		}
	
	public static void out(String st) { 
		System.out.print(st);
		}
	
	public  static void EXC(Exception E,String dove) {
		echo("\n\nException: "+dove+" = "+E.toString()+"\n"+E.getMessage()+"\n"+E.getLocalizedMessage()+"\n");
							StackTraceElement[] S = E.getStackTrace();
							for (int ax=0;ax<S.length;ax++) echo("STACK "+ax+":\t "+S[ax].toString()+"\n");
		}
	
	public static void file_put_bytes(String name,byte[]  data) throws Exception {
			FileOutputStream fo = new FileOutputStream(name);
			fo.write(data);
			fo.close();
		}	

	public void startKernel() {
		if (Main.Kernel!=null) return;
		ScheduledExecutorService K = Executors.newSingleThreadScheduledExecutor();
		Runnable KernelImpl = new Runnable() {
			@Override
			public void run() {
				try {	
					int ctask=0;
					int maxt=0;
					int grbt=0;
					tcrd = System.currentTimeMillis()/86400000L;
					if (tcrd!=cday) {
						cday=tcrd;
						Main.RandLog=Stdio.NewRndLong();
						}
					
					String nodup=",";
					//if (Config.Debug) Config.GlobalLog(Config.GLOG_All, "Kernel", "StartGarbage");
					if (Main.SMTPS!=null) {
							int cx = Main.SMTPS.length;
							maxt+=cx;
							for (int ax=0;ax<cx;ax++) try {
								if (Main.SMTPS[ax]!=null) {
									grbt++;
									if (!nodup.contains(","+Main.SMTPS[ax].Identity.Nick+",")) {
										ctask+=Main.SMTPS[ax].Identity.statsRunningPOP3Session;
										ctask+=Main.SMTPS[ax].Identity.statsRunningSMTPSession;
										nodup+=Main.SMTPS[ax].Identity.Nick+",";
										}
									Main.SMTPS[ax].Garbage();
									Main.SMTPS[ax].Identity.VoucherLogGarbage();
									}
								} catch(Exception KP) { Config.EXC(KP, "Kernel:SMTP"); }
							} //smtp
					
					if (Main.POP3S !=null) {
							int cx = Main.POP3S .length;
							maxt+=cx;
							for (int ax=0;ax<cx;ax++) try {
								if (Main.POP3S [ax]!=null) {
									grbt++;
									if (!nodup.contains(","+Main.POP3S [ax].Identity.Nick+",")) {
										ctask+=Main.POP3S [ax].Identity.statsRunningPOP3Session;
										ctask+=Main.POP3S [ax].Identity.statsRunningSMTPSession;
										nodup+=Main.POP3S [ax].Identity.Nick+",";
										}
									Main.POP3S [ax].Garbage();
									}
								} catch(Exception KP) { Config.EXC(KP, "Kernel:POP3"); }
							} //pop3
					
					if (Main.CSP !=null) {
							int cx = Main.CSP .length;
							maxt+=cx;
							for (int ax=0;ax<cx;ax++) try {
								if (Main.CSP [ax]!=null) {
									grbt++;
									Main.CSP [ax].Garbage();
									}
								} catch(Exception KP) { Config.EXC(KP, "Kernel:ControlPort"); }
							} //pop3
					
					try { ExtraThread.doGarbage(); } catch(Exception KP) { Config.EXC(KP, "Kernel:ExtraThread"); }
					
					if (Main.ListThreads !=null) {
								int cx = Main.ListThreads.length;
								maxt+=cx;
								long tcr = System.currentTimeMillis();
								
								for (int ax=0;ax<cx;ax++) try {
									if (Main.ListThreads[ax]==null) continue;
									boolean remove=false;
									grbt++;
									if ((tcr - Main.ListThreads[ax].Started)>Config.ListThreadsTTL) remove=true;
									if (Main.ListThreads[ax].running==false) remove=true;
									if (!Main.ListThreads[ax].isAlive()) remove=true;
									if (remove) {
										Main.ListThreads[ax].End();
										Main.ListThreads[ax]=null;
										} else ctask++;
									} catch(Exception KP) { Config.EXC(KP, "Kernel:ListThread"); }
							} //list
					
					if (Main.MultiTthread !=null) {
								int cx = Main.MultiTthread.length;
								maxt+=cx;
								long tcr = System.currentTimeMillis();
								
								for (int ax=0;ax<cx;ax++) try {
									if (Main.MultiTthread[ax]==null) continue;
									boolean remove=false;
									grbt++;
									if ((tcr - Main.MultiTthread[ax].Started)>Config.ListThreadsTTL) remove=true;
									if (Main.MultiTthread[ax].running==false) remove=true;
									if (!Main.MultiTthread[ax].isAlive()) remove=true;
									if (remove) {
										Main.MultiTthread[ax].End();
										Main.MultiTthread[ax]=null;
										} 
									ctask++;
									} catch(Exception KP) { Config.EXC(KP, "Kernel:MultiThread"); }
							} //MultiDeliver
					
						if (TextCaptcha.isEnabled()) try  { TextCaptcha.Garbage(false); } catch(Exception E) { Config.EXC(E, "TextCaptcha.Garbage"); }
					
						if (Main.Config.BlackListFile!=null) try {
							String ips="";
							
							for (SrvIdentity srv : Config.SMPTServer ) {
								if (srv==null) continue;
								if (!srv.EnterRoute) continue;
								if (srv.BlackList==null) continue;
								IPList lst = srv.BlackList;
								int j = lst.IPS.length;
								for (int i=0;i<j;i++) {
									if (lst.IPS[i]==0 || lst.Point[i]<Main.Config.BlackListLevel) continue;
									ips+=lst.getIPbyId(i)+"\t"+srv.Nick+"\t"+ (srv.ExitIP!=null ? J.IP2String(srv.ExitIP) : "!")+"\n";
									}
								}
							
							int chk = ips.hashCode();
							if (Main.preventTooBlackWrite!=chk) {
								String out="";
								ips=ips.trim();
								String[] lst = ips.split("\\n+");
								for (String li : lst) {
									String[] tk = li.split("\\t");
									String lo = Main.Config.BlackListMask.replace("${IP}", tk[0]);
									lo=lo.replace("${NICK}", tk[1]);
									lo=lo.replace("${SRVIP}", tk[2]);
									out+=lo+"\n";
									}
								
								if (lst.length!=Main.mBlackListIPs) Config.GlobalLog(Config.GLOG_All, "BlackListFile", "BlackListed "+lst.length+" ip");
								Main.mBlackListIPs=lst.length;
								
								ips=null;
								lst=null;
								Main.preventTooBlackWrite=chk;
								Stdio.file_put_bytes(Main.Config.BlackListFile, out.getBytes());
								}
							
							} catch(Exception E) { Config.EXC(E, "Kernel.BlackListFile"); }
						
						int cx = Main.HTTP.length;
						
						for (int ax=0;ax<cx;ax++) try {
							if (HTTP[ax]==null) continue;
							HTTP[ax].Garbage();
							} catch(Exception KH) { Config.EXC(KH, "Kernel:HTTP"); }
						
						Main.MaxThread = ctask;
						Main.PercThread = (int) Math.ceil(100.0*(grbt / maxt));
						if (Main.MaxThread>Main.statsMaxThread) Main.statsMaxThread=Main.MaxThread;
						if (Main.PercThread>Main.statsPercThread) Main.statsPercThread=Main.PercThread;
						//if (Config.Debug) Config.GlobalLog(Config.GLOG_All, "Kernel", ctask+" T.c., "+grbt+" T.r., "+Main.PercThread+"%");
				
						TheradsCounter(false,false);
						if (CurFuffaThreads>0) Config.GlobalLog(Config.GLOG_All, "Kernel", CurFuffaThreads+" dummy thread detected");
						cx = Config.SMPTServer.length;
						
						if (Config.UseStatus && Main.statusHash==null)  Main.statusHash = new long[cx];
						for (int ax=0;ax<cx;ax++) {
								if (Config.SMPTServer==null)  continue;
								Config.SMPTServer[ax].Garbage();
								if (!Config.UseStatus) continue;
								long t = Config.SMPTServer[ax].Status;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statsMaxRunningPOP3Session;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statsMaxRunningSMTPSession;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statMaxExit;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statMaxExitTrust;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statMaxExitBad;
								t^=t<<8;
								t+=Config.SMPTServer[ax].statMaxExitDown;
								t^=t<<8;
								if (t!=Main.statusHash[ax]) {
									Main.statusHash[ax]=t;
									String st = "StatusF:\t";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_Booting)!=0) st+="B";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_BootOk)!=0) st+="O";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_Error)!=0) st+="E";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_FriendOk)!=0) st+="F";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_FriendRun)!=0) st+="I";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_Loaded)!=0) st+="L";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_NotLoaded)!=0) st+="N";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_Ok)!=0) st+="K";
									if ((Config.SMPTServer[ax].Status&SrvIdentity.ST_Running)!=0) st+="L";
									if (Config.SMPTServer[ax].Status==0) st+="?";
									st+="\n";
									st+="POP3Max:\t"+Config.SMPTServer[ax].statsMaxRunningPOP3Session+"\n";
									st+="SMTPMax:\t"+Config.SMPTServer[ax].statsMaxRunningSMTPSession+"\n";
									st+="MaxExit:\t"+Config.SMPTServer[ax].statMaxExit+"\n";
									st+="MaxExitTrust:\t"+Config.SMPTServer[ax].statMaxExitTrust+"\n";
									st+="MaxExitBad:\t"+Config.SMPTServer[ax].statMaxExitBad+"\n";
									st+="MaxExitDown:\t"+Config.SMPTServer[ax].statMaxExitDown+"\n";
									long tcr = System.currentTimeMillis()+Config.TimeSpoof;
									st+="UpdateTCR:\t"+Long.toString(tcr/1000L)+"\n";
									st+="UpdateTime:\t"+new Date(tcr).toGMTString()+"\n";
									
									try {
										Stdio.file_put_bytes(Config.SMPTServer[ax].Maildir+"/status", st.getBytes());
										} catch(Exception FE) {
											Config.SMPTServer[ax].Log("Can't writwe status: "+FE.getMessage());
										}
									}
								}
						
					} catch(Exception KP) { Config.EXC(KP, "Kernel"); }	
				
				if (Config.TORCheckPolling>0) try {
					
					int tcr = (int) (System.currentTimeMillis()/1000L);
					if (tcr<TORCheckPollingRefreshRateTCR) return;
					TORCheckPollingRefreshRateTCR = tcr + Config.TORCheckPolling*60;
					
					PollingAllTest();
					
					} catch(Exception EP) { Config.EXC(EP, "Main.PollingAllTest"); }
				
				if (Config.SSLEDisabled) return;
				
				int tcr = (int) (System.currentTimeMillis()/1000L);
				if (tcr<SSLETRefreshRateTCR) return;
				SSLETRefreshRateTCR = tcr + Config.SSLETRefreshRate*60;
				
				try { 
					boolean changeid=false;
					int cx = Main.Config.SMPTServer.length;
					for (int ax=0;ax<cx;ax++) {
						if (Main.Config.SMPTServer[ax]==null) continue;
						
						try {
							Main.Config.SMPTServer[ax].CheckSSLOperations();
							} catch(Exception KPS) { Config.EXC(KPS, "Kernel.SSL:"+Main.Config.SMPTServer[ax].Nick); }
						
						changeid|=Main.Config.SMPTServer[ax].iWantChangeIdentity;
						Main.Config.SMPTServer[ax].iWantChangeIdentity=false;
						}
					
					if (changeid) {
							Config.GlobalLog(Config.GLOG_All, "Kernel.SSL", "Change TOR identity request.");
						if (Config.TORControlConf) {
								ChangeTORIdentity(Config);
								Config.TORIdentityNumber = 0x7FFFFFFF & (Config.TORIdentityNumber+1);
								if (Config.TORIdentityNumber==0) Config.TORIdentityNumber=1;
								Config.GlobalLog(Config.GLOG_All, "Kernel.SSL", "TOR identity changed: `"+Config.TORIdentityNumber+"`");
							} else {
								Config.TORIdentityNumber = 0x7FFFFFFF & (Config.TORIdentityNumber+1);
								if (Config.TORIdentityNumber==0) Config.TORIdentityNumber=1;
								Config.GlobalLog(Config.GLOG_All, "Kernel.SSL", "Warning: Dummy TOR identity change: `"+Config.TORIdentityNumber+"`");
							}
						}
					} catch(Exception KP) { Config.EXC(KP, "Kernel.SSL/TORID"); }
				}/*run*/
			};
		
		Main.Kernel=K;
		K.scheduleAtFixedRate(KernelImpl,1 ,60, TimeUnit.SECONDS);
		
	}
	
	private static void cerateRemotePasshpraseFile(String localFile,String oni,byte[] pwl) throws Exception {
		byte[] raw = Stdio.MxAccuShifter(new byte[][] {
				oni.toLowerCase().trim().getBytes() ,
				pwl			}, Const.MX_RemotePhFile) ;
		Stdio.file_put_bytes(localFile, raw);
		}
	
	private static void createStarterFile(String localFile,String pwl, String srvPwl, byte[] pwlpwl,String onix) throws Exception {
		byte[] rnd = new byte[128];
		Stdio.NewRnd(rnd);
		byte[] k1 = J.Der2048(rnd,pwl.getBytes());
		byte[] raw = Stdio.MxAccuShifter(new byte[][] {
			"START".getBytes() ,
			srvPwl.getBytes() , //passphrase server 20000
			pwlpwl, //password auth.
			onix.getBytes()}
			, Const.MX_RemotePhFile,true);
		raw = Stdio.EncMulti(k1, raw);
		
		raw = Stdio.MxAccuShifter(new byte[][] { rnd,  raw } , Const.MX_RemotePhFileS);
		Stdio.file_put_bytes(localFile, raw);
		}
	
	private static void starterCreation() throws Exception {
		Main.echo("Starter file creation utility\n");
		BufferedReader In = J.getLineReader(System.in);
		Main.echo("\tFile to put on OnionMail Server:> ");
		String startClient = In.readLine();
		Main.echo("\tFile to put on Password Server:> ");
		String startServer = In.readLine();
		
		Main.echo("\tOnion address of starter:> ");
		String oni = In.readLine();
		oni=oni.toLowerCase().trim();
		if (!oni.endsWith(".onion")) throw new Exception("Invalid address\n");
		Main.echo("\tOnion port:> ");
		String li = In.readLine();
		li=li.trim();
		int port = J.parseInt(li);
		if (port==0) throw new Exception("Invalid port");
				
		String spw;
		if (SelPass) {
			Main.echo("\tOnionMail's server password (-p) :> ");
			spw = In.readLine();
			spw=spw.trim();
			} else {
				spw = J.GenPassword(4096, 4096);
				Main.echo("4096 Char. password generated.\n");
				}
		byte[] spp = new byte[64];
		Stdio.NewRnd(spp);
		Main.echo("\tPassphrase:> ");
		li = In.readLine();
		li=li.trim();
		
		String onix=Integer.toString(port)+"."+oni;
		createStarterFile(startServer,li,spw,spp,onix);
		li=null;
		spw=null;
		cerateRemotePasshpraseFile(startClient,onix,spp);
		Main.echo("Files saved OK.\n");
		}
		
	private static String getRemotePassphrase(String remoteFile,Config C) throws Exception {
		Socket SK=null;
		InputStream I=null;
		OutputStream O=null;
		String p=null;
		try {
			if (C.Debug) Main.echo("Load remote passphrase from `"+remoteFile+"`\n\t"); else Main.echo("Load remote passphrase ... ");
			
			byte[] raw = Stdio.file_get_bytes(remoteFile);
			byte[][] F = Stdio.MxDaccuShifter(raw, Const.MX_RemotePhFile);
			raw=null;
			String oni = new String(F[0]);
			oni=oni.toLowerCase().trim();
			if (!oni.endsWith(".onion")) throw new Exception("@Invalid loader HOST");
			XOnionParser X = XOnionParser.fromString(C,oni);
			byte[] pwl = F[1];
			Main.echo("Connect ");
			SK = J.IncapsulateSOCKS(C.TorIP, C.TorPort, X.Onion, X.Port);
			I = SK.getInputStream();
			O = SK.getOutputStream();
			
			byte[] rnd = new byte[16];
			I.read(rnd);
			Main.echo("Authenticate ");
			raw = Stdio.md5a(new byte[][] { rnd, pwl });
			O.write(raw);
			
			int st = I.read();
			if (st!=1) throw new Exception("@Remote rejection code "+Integer.toString(st&255,16));
			Main.echo("GetKey ");
			
			byte[] bup = new byte[512];
			byte[] hl=new byte[2];
			I.read(hl);
			int sz = Stdio.Peek(0, hl);
			I.read(bup);
					
			bup = J.Der2048(
					pwl  ,
					bup	) 
					;
			
			raw = new byte[sz];
			I.read(raw);
			raw = Stdio.DecMulti(bup, raw);
			Main.echo("Processing ");
			F = Stdio.MxDaccuShifter(raw, Const.MX_RemotePhFile);
			if (new String(F[0]).compareTo("START")!=0) throw new Exception("@Wrong remote password");
			p = new String(F[1]);
			J.WipeRam(F);
			F=null;
			J.WipeRam(raw);
			raw=null;
			O.write(1);
			if (O!=null) try { O.close(); } catch(Exception Ie) {}
			if (I!=null) try { I.close();  } catch(Exception Ie) {}
			if (SK!=null) try { SK.close();  } catch(Exception Ie) {}
			Main.echo("OK\n");
			} catch(Exception E) {
				try { O.close(); } catch(Exception Ie) {}
				try { I.close();  } catch(Exception Ie) {}
				try { SK.close();  } catch(Exception Ie) {}
				Main.echo("Error!\n");
				String e = E.getMessage();
				if (e!=null && e.startsWith("@")) Main.echo("Error: "+e.substring(1)+"\n"); else {
						Main.echo("Error: Unknown\n");
						if (C.Debug) E.printStackTrace();
						}
				throw E;
			}
		return p;
		} 
	
	private static void beginPassphraseServer(InetAddress localIP, String serverFile,Config C) throws Exception {
		Main.echo("Passphrase server mode:\n");
		Socket con=null;
		InputStream I =null;
		OutputStream O=null;
		ServerSocket  srv=null;
		try {
			byte[] raw = Stdio.file_get_bytes(serverFile);
			byte[][] F = Stdio.MxDaccuShifter(raw, Const.MX_RemotePhFileS);
			
			Main.echo("\tPassphrase:> ");
			BufferedReader In = J.getLineReader(System.in);
			String pw = In.readLine();
			pw=pw.trim();
			byte[] k1 = J.Der2048(F[0],pw.getBytes());
																	
			raw = Stdio.DecMulti(k1, F[1]);
			pw=null;
			F = Stdio.MxDaccuShifter(raw,Const.MX_RemotePhFile);
			if (new String(F[0]).compareTo("START")!=0) throw new Exception("Key Error");
			
			XOnionParser X = XOnionParser.fromString(C,new String (F[3]));
			srv = new ServerSocket(X.Port,0,localIP);
			Main.echo("Listening\n");
			con = srv.accept();
			I = con.getInputStream();
			O = con.getOutputStream();
			Main.echo("Incoming connection ");
			byte[] rnd = new byte[16];
			Stdio.NewRnd(rnd);
			O.write(rnd);
			byte[] chk = Stdio.md5a(new byte[][] { rnd, F[2] });
			raw=new byte[16];
			I.read(raw);
			if (!Arrays.equals(raw, chk)) {
				O.write(2);
				throw new Exception("@Wrong remote password!\b\n");
				}
			
			Main.echo("Auth_OK\n\tAuthorize (YES/NO)? >");
			while(true) {
				pw = In.readLine();
				pw=pw.toLowerCase().trim();
				if (pw.compareTo("no")==0) {
					O.write(3);
					throw new Exception("@Access denied by user!\b\n");
					}
				if (pw.compareTo("yes")==0) {
					O.write(1);
					break;
					}
				Main.echo("(YES/NO)? ");
				}
			
			Main.echo("Sending key ... ");
			
			byte[] bup=new byte[512];
			Stdio.NewRnd(bup);
				
			byte[] key = J.Der2048(
						F[2] ,
						bup	) 
						;
			
			raw = Stdio.MxAccuShifter(new byte[][] {
					"START".getBytes(),
					F[1]		
					}, Const.MX_RemotePhFile,true ) 
					;
						
			raw = Stdio.EncMulti(key, raw);
			byte[] hl = new byte[2];
			Stdio.Poke(0, raw.length, hl);
			O.write(hl);
			O.write(bup);
			O.write(raw);
			bup=null;
			raw=null;
			F=null;
			if (I.read()!=1) throw new Exception("@Remote server is not started!");
			Main.echo("OK\n");
			} catch(Exception E) {
			if (O!=null) try { O.close(); } catch(Exception Ie) {}
			if (I!=null) try { I.close();  } catch(Exception Ie) {}
			if (con!=null) try { con.close();  } catch(Exception Ie) {}
			if (srv!=null) try { srv.close();  } catch(Exception Ie) {}
			String m = E.getMessage();
			if (m!=null && m.startsWith("@")) {
				Main.echo("Error!\n\tError: "+m.substring(1)+"\n");
				return;
				} else {
				Main.echo("Error!\n");
				if (C.Debug) E.printStackTrace();
				}
			}
		try { O.close(); } catch(Exception Ie) {}
		try { I.close();  } catch(Exception Ie) {}
		try { con.close();  } catch(Exception Ie) {}
		try { srv.close();  } catch(Exception Ie) {}
		}
			
	private static void BatchMode() throws Exception {
		Main.out("OM:[DATA] Stdin headers\n");
		BufferedReader In = J.getLineReader(System.in);
		ConfVars = J.ParseHeaders(In);
		Main.out("OM:[DATA_OK] Processing\n");
		if (ConfVars.containsKey("global-pass")) Main.SetPass = ConfVars.get("global-pass");
		if (ConfVars.containsKey("global-selpass")) Main.SelPass = Config.parseY(ConfVars.get("global-pass"));
		if (ConfVars.containsKey("global-setpgp-root")) Main.PGPRootMessages = Config.parseY(ConfVars.get("global-setpgp-root"));
		if (ConfVars.containsKey("global-setpgp")) Main.SetPGPSrvKeys = Config.parseY(ConfVars.get("global-setpgp"));
		if (ConfVars.containsKey("global-setndk")) Main.NoDelKeys = Config.parseY(ConfVars.get("global-setndk"));
		if (ConfVars.containsKey("global-echo")) Main.echo (ConfVars.get("global-echo")+"\n");
		if (ConfVars.containsKey("global-stderr")) System.err.print(ConfVars.get("global-stderr")+"\n");
	}
		
	//XXX Mettere nelle statistiche
	public static volatile short CurThreads = 0;
	public static volatile short MaxThreads = 0;
	public static volatile short CurFuffaThreads = 0;
	public static volatile short MaxFuffaThreads = 0;
	public static volatile int StatsKCurHour=0;
	public static volatile short[] StatsKThreadsXHour = new short[72];
	
	public static String TheradsCounter(boolean ret,boolean byMain) throws Exception {
		int tcr = (int)(System.currentTimeMillis()/3600000L);
		if (tcr!=StatsKCurHour) {
			StatsKCurHour=tcr;
			int cx = StatsKThreadsXHour.length-1;
			System.arraycopy(StatsKThreadsXHour, 1, StatsKThreadsXHour, 0, cx);
			StatsKThreadsXHour[cx] =MaxThreads;
			MaxThreads=0;
			MaxFuffaThreads=0;
			}
		
		String rs = ret ? "" : null;
		Set<Thread> tutti = Thread.getAllStackTraces().keySet();
		int cx = tutti.size();
		Thread[] Tutto = tutti.toArray(new Thread[cx]);
		tutti=null;
		CurThreads=(short) cx;
		if (CurThreads>MaxThreads) MaxThreads=CurThreads;
		short fuffaThread=0;
		
		for (int ax=0;ax<cx;ax++) {
			boolean isFuffa=false;
			if (
						Tutto[ax].isInterrupted() 	|| 
						!Tutto[ax].isAlive()				) {
							fuffaThread++;
							isFuffa=true;
							}
			
			if (ret) {
				String cl  = Tutto[ax].getClass().getSimpleName();
				if (byMain) {
					if (!" ControlSession MailQueueSender MultiDeliverThread ListThread SrvHTTPRequest SrvPop3Session SrvSMTPSession ".contains(" "+cl+" ")) continue; 
					}
				rs+=Long.toString(Tutto[ax].getId(),36)+"\t"+(isFuffa ? "-":"R") + "\t";
				rs+=cl+"\n";
				}
			}		
		
		CurFuffaThreads=fuffaThread;
		if (CurFuffaThreads>MaxFuffaThreads) MaxFuffaThreads=CurFuffaThreads;
		return rs;
	}
	
	public void PollingAllTest() throws Exception {
		int[] ts = SelfTest(false, false);

		boolean ch = false;
		int cx = ts.length;
		for (int ax=0;ax<cx;ax++) {
			if (ts[ax]==Main.STEST_POP3SOCK)  {ch=true; break; }
			if (ts[ax]==Main.STEST_SMTPE)  {ch=true; break; }
			}
		if (!ch) return;
		Config.GlobalLog(Config.GLOG_All | Config.GLOG_Event, "MAIN", "Some servers are unreachable." + (Config.TORControlConf ? "Change TOR identity." : "I need to change TOR identity but TOR control port is not configured"));
		if (Config.TORControlConf) Main.ChangeTORIdentity(Config);
	}
	
	@SuppressWarnings("resource")
	public static void ChangeTORIdentity(Config Config) throws Exception {
		String ip;
		if (Config.TORControlIP!=null ) ip = Config.TORControlIP; else  ip ="127.0.0.1";
		Socket t = null;
		t= new Socket(ip,Config.TORControlPort);
		BufferedReader I = null;
		OutputStream O = null;
		Exception E=null;
		try {
			I = J.getLineReader(t.getInputStream());
			O = t.getOutputStream();
			String s = "AUTHENTICATE";
			if (Config.TORControlPass!=null) s+=" \""+Config.TORControlPass+"\"";
			s+="\r\n";
			O.write(s.getBytes());
			String i = I.readLine();
			if (!i.startsWith("250 ")) throw new Exception(i.trim());
			s = "SIGNAL NEWNYM\r\n";
			O.write(s.getBytes());
			i = I.readLine();
			if (!i.startsWith("250 ")) throw new Exception(i.trim());
			s = "QUIT\r\n";
			O.write(s.getBytes());
			i = I.readLine();
			} catch(Exception Ei) {E=Ei;}
		
		try { if (t!=null) t.close(); } catch(Exception EI) {}
		try { if (O!=null) O.close(); } catch(Exception EI) {}
		try { if (I!=null) I.close(); } catch(Exception EI) {}
		if (E!=null) throw E;
	}
	
	private static void srvPasswd(String path,String pwlf,boolean ba) throws Exception {
		if (!path.endsWith("/")) path+="/";
		path+="keyblock.txt";
		if (!new File(path).exists()) {
			echo("Can't find `"+path+"`\n");
			Main.endProc(1);
			}
	
		byte[] b = Stdio.file_get_bytes(path);
		b = J.ASCIISequenceRead(new String(b),"KEYBLOCK");
		BufferedReader In = J.getLineReader(System.in);
		
		byte[] oldp;
		if (pwlf==null) {
			if (ba) echo("OM:[OLDBOOTP]\n"); else echo("Enter old boot password: > ");
			String op = In.readLine();
			op=op.trim();
			oldp=op.getBytes();
		} else {
			FileInputStream si = new FileInputStream(pwlf);
			try {
				BufferedReader x = J.getLineReader(si);
				String pw = x.readLine();
				pw=pw.trim();
				oldp=pw.getBytes();
				} catch(Exception e) {
				try { si.close(); } catch(Exception i) {}
				throw e;
				}
			si.close();
			}
			
		byte[] newp=null;
		byte[][] ks = SrvIdentity.KSDecode(b, oldp);
		
		if (ba) echo("OM:[NEWBOOTP]\n"); else echo("Enter new boot password: > ");
		String op=In.readLine();
		op=op.trim();
		newp=op.getBytes();
				
		byte[] t2 =SrvIdentity.KSEncode(ks, newp);
		String p2 = J.ASCIISequenceCreate(t2, "KEYBLOCK");
		Stdio.file_put_bytes(path, p2.getBytes());
		if (ba) echo("OM:[OK]\n"); else echo("Boot password changed\n");
		
	}
	
	private static void EXCM(Exception E) {
		String msg = E.getMessage();
		if (msg==null) msg="Unknown";
		if (msg.startsWith("@")) msg=msg.substring(1);
		if (msg.startsWith("_")) msg="Wrong key in MxDaccuShifter: "+msg.substring(1);
		Main.echo("Error: "+msg+"\n");
		if (verbose) E.printStackTrace();
		Main.endProc(1);
		}
	
	
	public static void endProc(int s) {
		try {
			Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Begin endProc");
			long orm = Main.getUsedMememory();
			
			int cx=0;
			if (Main.POP3S!=null) {
				cx = Main.POP3S.length;
				for (int ax=0;ax<cx;ax++) {
					try {	
						Main.POP3S[ax].End();
						} catch(Exception E) { Config.EXC(E, "Term `"+Main.POP3S[ax].Identity.Onion+"`"); }
					}
			}
			if (Main.SMTPS!=null) {
				cx = Main.SMTPS.length;
				for (int ax=0;ax<cx;ax++) {
					try {	
						Main.SMTPS[ax].End();
						} catch(Exception E) { Config.EXC(E, "Term `"+Main.SMTPS[ax].Identity.Onion+"`"); }
					}
			}
			
			if (Main.CSP!=null) {
				cx = Main.CSP.length;
				for (int ax=0;ax<cx;ax++) {
					try {	
						Main.CSP[ax].End();
						} catch(Exception E) { Config.EXC(E, "Term `"+Main.CSP[ax].Identity[0].Onion+"`"); }
					}
				}
			
			if (Main.CS!=null) try {	
						Main.CS.End();
					} catch(Exception E) { Config.EXC(E, "Term ControlPort"); }
			
			if (Main.ETH!=null) try {
				cx =Main.ETH.length;
				for (int ax=0;ax<cx;ax++) {
					if (Main.ETH[ax]==null) continue;
					Main.ETH[ax].end();
					}
				} catch(Exception i) {}
			
			if (Main.HTTP!=null) {
				cx=Main.HTTP.length;
				for (int ax=0;ax<cx;ax++) try {
					if (Main.HTTP[ax]==null) continue;
					Main.HTTP[ax].End();
				} catch(Exception i) {}
			}
			
			if (Main.ListThreads!=null) {
				cx=Main.ListThreads.length;
				for (int ax=0;ax<cx;ax++) try {
					if (Main.ListThreads[ax]==null) continue;
					Main.ListThreads[ax].End();
					} catch(Exception I) {}
			}
			
			if (Main.MultiTthread!=null) {
				cx=Main.MultiTthread.length;
				for (int ax=0;ax<cx;ax++) try {
					if (Main.MultiTthread[ax]==null) continue;
					Main.MultiTthread[ax].End();
					} catch(Exception I) {}
			}
					
			Main.ETH=null;
			Main.HTTP=null;
			Main.CSP=null;
			Main.CS=null;
			Main.DNSCheck=null;
			Main.FSKA=null;
			Main.FSKIV=null;
			if (Main.Kernel!=null) Main.Kernel.shutdown();
			Main.Kernel=null;
			Main.IDK=null;
			Main.FSK=null;
			Main.ListThreads=null;
			Main.MultiTthread=null;
			Main.POP3S=null;
			Main.SMTPS=null;
			Main.Config.SMPTServer=null;
			Main.Config.ResevedUserKey=null;
					
			long cmem=0;
			try { Thread.sleep(500); } catch (InterruptedException e) { }
			
			if ((Main.cmdFlags & Main.CF_X_FASTEXIT)!=0) {
				System.gc();
				if (Main.furamidal!=null) {
				orm=Main.getUsedMememory();
				Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Furamide");
				Main.furamidal.endProc();
				Main.furamidal.close();
				cmem=Main.getUsedMememory();
				Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Furamide for "+Integer.toString((int) (Math.abs(cmem-orm)/1024))+" KB");
				}
				System.gc();
				Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: OnionMail FastExit Code="+s);
				System.exit(s);
			}
			
			for (int ax=0;ax<5;ax++) {
				System.gc();
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
				System.gc();
				cmem=Main.getUsedMememory();
				if (cmem<orm) break;
			}
			long diff= orm-cmem;
			Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Free "+Long.toString(diff/1024)+" KB");		
			if (cmem<orm && (Main.cmdFlags&Main.CF_Z_NOZEROEND)==0) {
				Runtime runtime = Runtime.getRuntime();
				long mxm = (long)(runtime.maxMemory()*0.7);
				if (diff<mxm) try {
					Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: ZeroMemory "+Long.toString(diff/1024)+" KB");
					byte[] a = new byte [(int)diff];
					orm=a.hashCode();
					a=null;
					System.gc();
					try { Thread.sleep(100); } catch (InterruptedException e) { }
				} catch(Exception f) {
					System.gc();
					Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: ZeroMememory Error: "+f.getMessage());
				}
			}		
			
			orm=Main.getUsedMememory();
			for (int ax=0;ax<5;ax++) {
				System.gc();
				try { Thread.sleep(200); } catch (InterruptedException e) { }
				System.gc();
				cmem=Main.getUsedMememory();
				if (cmem<orm) break;
			}
			
			if (Main.furamidal!=null) {
				orm=Main.getUsedMememory();
				Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Furamide");
				Main.furamidal.endProc();
				Main.furamidal.close();
				cmem=Main.getUsedMememory();
				Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Furamide for "+Integer.toString((int) (Math.abs(cmem-orm)/1024))+" KB");
			}
			System.gc();
						
			Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: OnionMail Exit Code="+s);
		} catch(Exception I) {
			Main.Config.GlobalLog(Config.GLOG_All, "MAIN", "Endproc: Error: "+I.getMessage());
			I.printStackTrace();
			try {} catch(Exception j) {}
		}
		System.exit(s);
	}
		
	protected static void ZZ_Exceptionale() throws Exception { throw new Exception(); } //Remote version verify
}
//NTRU ???