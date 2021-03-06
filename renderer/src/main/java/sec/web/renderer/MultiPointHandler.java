package sec.web.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sec.web.json.utilities.JSONArray;
import sec.web.json.utilities.JSONException;
import sec.web.json.utilities.JSONObject;
import sec.web.renderer.utilities.JavaRendererUtilities;
import sec.web.renderer.utilities.LineInfo;
import sec.web.renderer.utilities.PNGInfo;
import sec.web.renderer.utilities.SymbolInfo;
import sec.web.renderer.utilities.TextInfo;
import ArmyC2.C2SD.RendererPluginInterface.ISinglePointInfo;
import ArmyC2.C2SD.Rendering.MultiPointRenderer;
import ArmyC2.C2SD.Utilities.ErrorLogger;
import ArmyC2.C2SD.Utilities.IMultiPointRenderer;
import ArmyC2.C2SD.Utilities.IPointConversion;
import ArmyC2.C2SD.Utilities.MilStdSymbol;
import ArmyC2.C2SD.Utilities.ModifiersTG;
import ArmyC2.C2SD.Utilities.PointConversion;
import ArmyC2.C2SD.Utilities.RendererSettings;
import ArmyC2.C2SD.Utilities.ShapeInfo;
import ArmyC2.C2SD.Utilities.SymbolUtilities;
import JavaTacticalRenderer.TGLight;
import RenderMultipoints.clsRenderer;


@SuppressWarnings({"unused","rawtypes","unchecked"})
public class MultiPointHandler {

    private final static String MODIFIER_HEADER = "modifiers";
    private final static String QUANTITY = "quantity";//C
    private final static String REINFORCE_OR_REDUCED = "reinforcedOrReduced";
    private final static String STAFF_COMMENTS = "staffComments";
    private final static String ADDITIONAL_INFO_1 = "additionalInfo1";//H
    private final static String ADDITIONAL_INFO_2 = "additionalInfo2";//H1
    private final static String ADDITIONAL_INFO_3 = "additionalInfo3";//H2
    private final static String EVALUATION_RATION = "evaluationRating";
    private final static String COMBAT_EFFECTIVENESS = "combatEffectiveness";
    private final static String SIGNATURE_EQUIPMENT = "signatureEquipment";
    private final static String HIGHER_FORMATION = "higherFormation";
    private final static String HOSTILE = "hostile";//N
    private final static String IFFSIFF = "iffSif";
    private final static String DIRECTION_OF_MOVEMENT = "directionOfMovement";//Q
    private final static String UNIQUE_DESIGNATION_1 = "uniqueDesignation1";//T
    private final static String UNIQUE_DESIGNATION_2 = "uniqueDesignation2";//T1
    private final static String EQUIPMENT_TYPE = "equipmentType";//V
    private final static String DATE_TIME_GROUP_1 = "dateTimeGroup1";//W
    private final static String DATE_TIME_GROUP_2 = "dateTimeGroup2";//W1
    private final static String DATE_TIME_GROUP_3 = "dateTimeGroup3";//W2
    private final static String ALTITUDE_DEPTH = "altitudeDepth";//X
    private final static String LOCATION = "location";//Y
    private final static String SPEED = "speed";
    private final static String SPECIAL_C2_HQ = "specialC2Headquarters";
    private final static String DISTANCE = "distance";//AM
    private final static String AZIMUTH = "azimuth";//AN
    private final static String FILL_COLOR = "fillColor";
    private final static String LINE_COLOR = "lineColor";
    private final static String LINE_THICKNESS = "lineThickness";
    
    private final static String SYMBOL_FILL_ICON_SIZE = "symbolFillIconSize";
    private final static String SYMBOL_FILL_IDS = "symbolFillIds";
    private final static String SYMBOL_LINE_IDS = "symbolLineIds";
    
    /**
    * 2525Bch2 and USAS 13/14 symbology
    */
    public static final int Symbology_2525Bch2_USAS_13_14 = 0;
    /**
    * 2525C, which includes 2525Bch2 & USAS 13/14
    */
    public static final int Symbology_2525C = 1;
    
    /**
     * GE has the unusual distinction of being an application with coordinates outside its own extents.
     * It appears to only be a problem when lines cross the IDL
     * @param pts2d the client points
     */
    public static void NormalizeGECoordsToGEExtents(double leftLongitude,
            double rightLongitude,
            ArrayList<Point2D.Double>pts2d)
    {
        try
        {
            int j=0;
            double x=0,y=0;
            Point2D.Double pt2d=null;
            for(j=0;j<pts2d.size();j++)
            {
                pt2d=pts2d.get(j);
                x=pt2d.getX();
                y=pt2d.getY();
                while(x<leftLongitude)
                    x+=360;
                while(x>rightLongitude)
                    x-=360;

                pt2d=new Point2D.Double(x,y);
                pts2d.set(j, pt2d);
            }
        }
        catch (Exception exc)
        {

        }
    }
    /**
     * GE recognizes coordinates in the range of -180 to +180
     * @param pt2d
     * @return
     */
    private static Point2D NormalizeCoordToGECoord(Point2D pt2d)
    {
        Point2D ptGeo=null;
        try
        {
            double x=pt2d.getX(),y=pt2d.getY();
            while(x<-180)
                x+=360;
            while(x>180)
                x-=360;

            ptGeo=new Point2D.Double(x,y);
        }
        catch (Exception exc)
        {

        }
        return ptGeo;
    }
    /**
     * We have to ensure the bounding rectangle at least includes the symbol or
     * there are problems rendering, especially when the symbol crosses the IDL
     * @param controlPoints the client symbol anchor points
     * @param bbox the original bounding box
     * @return the modified bounding box
     */
    private static String getBoundingRectangle(String controlPoints,
            String bbox)
    {
        String bbox2="";
        try
        {
            //first get the minimum bounding rect for the geo coords
//            double minx=Double.MAX_VALUE, maxx=-Double.MAX_VALUE;
//            double miny=Double.MAX_VALUE, maxy=-Double.MAX_VALUE;
            Double left = 0.0;
            Double right = 0.0;
            Double top = 0.0;
            Double bottom = 0.0;
//            if(bbox != null && bbox.equals("")==false)
//            {
//                String[] bounds = bbox.split(",");
//
//                left = Double.valueOf(bounds[0]).doubleValue();
//                right = Double.valueOf(bounds[2]).doubleValue();
//                top = Double.valueOf(bounds[3]).doubleValue();
//                bottom = Double.valueOf(bounds[1]).doubleValue();
//            }
//            else
//                return null;

            String[] coordinates = controlPoints.split(" ");
            //ArrayList<Point2D.Double> geoCoords = new ArrayList();
            int len = coordinates.length;
            int i=0;
            left=Double.MAX_VALUE;
            right=-Double.MAX_VALUE;
            top=-Double.MAX_VALUE;
            bottom=Double.MAX_VALUE;
            for(i=0;i<len;i++)
            {
                String[] coordPair = coordinates[i].split(",");
                Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
                Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
                //geoCoords.add(new Point2D.Double(longitude, latitude));
                if(longitude<left)
                    left=longitude;
                if(longitude>right)
                    right=longitude;
                if(latitude>top)
                    top=latitude;
                if(latitude<bottom)
                    bottom=latitude;
            }
            bbox2=left.toString()+","+bottom.toString()+","+right.toString()+","+top.toString();
        }
        catch(Exception ex)
        {
            System.out.println("Failed to create bounding rectangle in MultiPointHandler.getBoundingRect");
        }
        return bbox2;
    }
    private static Rectangle.Double getControlPoint(String controlPoints, String bbox)
    {
        Rectangle.Double rect=null;
        try
        {
            //we need to adjust the bounding box to at least include the symbol
            String bbox2=getBoundingRectangle(controlPoints, bbox);
            String[] bounds2 = bbox2.split(",");
            double left = Double.valueOf(bounds2[0]).doubleValue();
            double right = Double.valueOf(bounds2[2]).doubleValue();
            double top = Double.valueOf(bounds2[3]).doubleValue();
            double bottom = Double.valueOf(bounds2[1]).doubleValue();
            double width=Math.abs(right-left);
            double height=Math.abs(top-bottom);
            rect=new Rectangle.Double(left, top, width, height);
        }
        catch(Exception ex)
        {
            System.out.println("Failed to create control point in MultiPointHandler.getControlPoint");
        }
        return rect;
    }
    /**
     * need to use the symbol to get the upper left control point
     * in order to produce a valid PointConverter
     * @param geoCoords
     * @return
     */
    private static Point2D getControlPoint(ArrayList<Point2D.Double>geoCoords)
    {
        Point2D pt2d=null;
        try
        {
            double left=Double.MAX_VALUE;
            double right=-Double.MAX_VALUE;
            double top=-Double.MAX_VALUE;
            double bottom=Double.MAX_VALUE;
            Point2D ptTemp=null;
            for(int j=0;j<geoCoords.size();j++)
            {
                ptTemp=geoCoords.get(j);
                if(ptTemp.getX()<left)
                    left=ptTemp.getX();
                if(ptTemp.getX()>right)
                    right=ptTemp.getX();
                if(ptTemp.getY()>top)
                    top=ptTemp.getY();
                if(ptTemp.getY()<bottom)
                    bottom=ptTemp.getY();
            }
            pt2d=new Point2D.Double(left,top);
        }
        catch(Exception ex)
        {
            System.out.println("Failed to create control point in MultiPointHandler.getControlPoint");
        }
        return pt2d;
    }
    /**
     * Assumes a reference in which the north pole is on top.
     * @param geoCoords the geographic coordinates
     * @return the upper left corner of the MBR containing the geographic coordinates
     */
    private static Point2D getGeoUL(ArrayList<Point2D.Double>geoCoords)
    {
        Point2D ptGeo=null;
        try
        {
            int j=0;
            Point2D pt=null;
            double left=geoCoords.get(0).x;
            double top=geoCoords.get(0).y;
            double right=geoCoords.get(0).x;
            double bottom=geoCoords.get(0).y;
            for(j=1;j<geoCoords.size();j++)
            {
                pt=geoCoords.get(j);
                if(pt.getX()<left)
                    left=pt.getX();
                if(pt.getX()>right)
                    right=pt.getX();
                if(pt.getY()>top)
                    top=pt.getY();
                if(pt.getY()<bottom)
                    bottom=pt.getY();
            }
            //if geoCoords crosses the IDL
            if(right-left>180)
            {
                //There must be at least one x value on either side of +/-180. Also, there is at least
                //one positive value to the left of +/-180 and negative x value to the right of +/-180.
                //We are using the orientation with the north pole on top so we can keep
                //the existing value for top. Then the left value will be the least positive x value
                left=geoCoords.get(0).x;
                for(j=1;j<geoCoords.size();j++)
                {
                    pt=geoCoords.get(j);
                    if(pt.getX()>0 && pt.getX()<left)
                        left=pt.getX();
                }
            }
            ptGeo=new Point2D.Double(left,top);
        }
        catch(Exception ex)
        {
            System.out.println("Failed to create control point in MultiPointHandler.getControlPoint");
        }
        return ptGeo;
    }
    private static boolean crossesIDL(ArrayList<Point2D.Double>geoCoords)
    {
        boolean result=false;
        Point2D pt2d=getControlPoint(geoCoords);
        double left=pt2d.getX();
        Point2D ptTemp=null;
        for(int j=0;j<geoCoords.size();j++)
        {
            ptTemp=geoCoords.get(j);
            if(Math.abs(ptTemp.getX()-left)>180)
                return true;
        }
        return result;
    }
    /**
     * Checks if a symbol is one with decorated lines which puts a strain
     * on google earth when rendering like FLOT.  These complicated lines
     * should be clipped when possible.
     * @param symbolID
     * @return 
     */
    public static Boolean ShouldClipSymbol(String symbolID)
    {
        String affiliation = SymbolUtilities.getStatus(symbolID);
        
        if(symbolID.substring(0, 1).equals("G") && affiliation.equals("A"))
        {
            //SymbolDef sd = SymbolDefTable.getInstance().getSymbolDef(symbolID);
            //if(sd.getDrawCategory()==SymbolDef.DRAW_CATEGORY_LINE ||
            //        sd.getDrawCategory()==SymbolDef.DRAW_CATEGORY_POLYGON)
            //{
                return true;
            //}
        }
        
        if(SymbolUtilities.isWeather(symbolID))
            return true;
        
        String id = SymbolUtilities.getBasicSymbolID(symbolID);
        if(id.equals("G*T*F-----****X") || 
                id.equals("G*F*LCC---****X") ||//CFL
                id.equals("G*G*GLB---****X") || 
                id.equals("G*G*GLF---****X") || 
                id.equals("G*G*GLC---****X") || 
                id.equals("G*G*GAF---****X") || 
                id.equals("G*G*AAW---****X") || 
                id.equals("G*G*DABP--****X") || 
                id.equals("G*G*OLP---****X") || 
                id.equals("G*G*PY----****X") || 
                id.equals("G*G*PM----****X") || 
                id.equals("G*G*ALL---****X") || 
                id.equals("G*G*ALU---****X") || 
                id.equals("G*G*ALM---****X") || 
                id.equals("G*G*ALC---****X") || 
                id.equals("G*G*ALS---****X") || 
                id.equals("G*G*SLB---****X") || 
                id.equals("G*G*SLH---****X") || 
                id.equals("G*G*GAY---****X") ||
                id.equals("G*M*OFA---****X") || 
                id.equals("G*M*OGB---****X") || 
                id.equals("G*M*OGL---****X") || 
                id.equals("G*M*OGZ---****X") || 
                id.equals("G*M*OGF---****X") || 
                id.equals("G*M*OGR---****X") || 
                id.equals("G*M*OADU--****X") || 
                id.equals("G*M*OADC--****X") || 
                id.equals("G*M*OAR---****X") || 
                id.equals("G*M*OAW---****X") || 
                id.equals("G*M*OEF---****X") ||  //Obstacles Effect Fix
                id.equals("G*M*OMC---****X") || 
                id.equals("G*M*OWU---****X") || 
                id.equals("G*M*OWS---****X") || 
                id.equals("G*M*OWD---****X") || 
                id.equals("G*M*OWA---****X") || 
                id.equals("G*M*OWL---****X") || 
                id.equals("G*M*OWH---****X") || 
                id.equals("G*M*OWCS--****X") || 
                id.equals("G*M*OWCD--****X") || 
                id.equals("G*M*OWCT--****X") || 
                id.equals("G*M*OHO---****X") || 
                id.equals("G*M*BDD---****X") || //Bypass Difficult
                id.equals("G*M*BCD---****X") || //Ford Difficult
                id.equals("G*M*BCE---****X") || //Ford Easy
                id.equals("G*M*SL----****X") || 
                id.equals("G*M*SP----****X") || 
                id.equals("G*M*NR----****X") || 
                id.equals("G*M*NB----****X") || 
                id.equals("G*M*NC----****X") || 
                id.equals("G*F*ACNI--****X") || 
                id.equals("G*F*ACNR--****X") || 
                id.equals("G*F*ACNC--****X") || 
                id.equals("G*F*AKBC--****X") || 
                id.equals("G*F*AKBI--****X") || 
                id.equals("G*F*AKBR--****X") || 
                id.equals("G*F*AKPC--****X") || 
                id.equals("G*F*AKPI--****X") || 
                id.equals("G*F*AKPR--****X") || 
                id.equals("G*F*LT----****X") || 
                id.equals("G*F*LTS---****X") || 
                id.equals("G*G*SAE---****X") || 
                //id.equals("G*G*SLA---****X") || //Ambush
                id.equals("G*S*LRA---****X") || 
                id.equals("G*S*LRM---****X") || 
                id.equals("G*S*LRO---****X") || 
                id.equals("G*S*LRT---****X") || 
                id.equals("G*S*LRW---****X") || 
                id.equals("G*T*Q-----****X") || 
                id.equals("G*T*E-----****X") || 
                id.equals("G*T*F-----****X") || //Tasks Fix
                id.equals("G*T*K-----****X") || //counterattack.
                id.equals("G*T*KF----****X") || //counterattack by fire.
                id.equals("G*G*PA----****X") || //AoA for Feint
                id.equals("G*T*A-----****X"))
        {
            return true;
        }
        else
            return false;
    }
    
    /**
     * 
     * @param id
     * @param name
     * @param description
     * @param symbolCode
     * @param controlPoints
     * @param scale
     * @param bbox
     * @param symbolModifiers
     * @param format
     * @return 
     */
    public static String RenderSymbol(String id,
            String name,
            String description,
            String symbolCode,
            String controlPoints,
            Double scale,
            String bbox,
            String symbolModifiers,
            int format)
    {
        return RenderSymbol(id, name, description, symbolCode, controlPoints, 
                scale, bbox, symbolModifiers, format, 
                RendererSettings.getInstance().getSymbologyStandard());
    }
    
    /**
     * Deutch updated version
     * @param id
     * @param name
     * @param description
     * @param symbolCode
     * @param controlPoints
     * @param scale
     * @param bbox
     * @param shapes
     * @param symStd 0=2525Bch2, 1=2525C
     */
    public static String RenderSymbol(String id,
            String name,
            String description,
            String symbolCode,
            String controlPoints,
            Double scale,
            String bbox,
            String symbolModifiers,
            int format, int symStd)//,
    //ArrayList<ShapeInfo>shapes)
    {
        boolean debug = false;
        if(debug)
        {
            System.out.println("MultiPointHander.RenderSymbol()");
            System.out.println("NAME: " + name);
            System.out.println("DESCRIPTION: " + description);
            System.out.println("SYMBOLID: " + symbolCode);
            System.out.println("POINTS: " + controlPoints);
            System.out.println("SCALE: " + String.valueOf(scale));
            System.out.println("BBOX: " + bbox);
            System.out.println("MODIFIERS: " + symbolModifiers);
            System.out.println("FORMAT: " + String.valueOf(format));
            System.out.println("SYMSTD: " + String.valueOf(symStd));
        }
        
        //System.out.println("MultiPointHandler.RenderSymbol()");
        boolean normalize = false;
        Double controlLat = 0.0;
        Double controlLong = 0.0;
        //Double metPerPix = GeoPixelConversion.metersPerPixel(scale);
        //String bbox2=getBoundingRectangle(controlPoints,bbox);
        StringBuilder jsonOutput = new StringBuilder();
        String jsonContent = "";

        Rectangle rect = null;

        //for symbol & line fill
        ArrayList<JavaLineArray.POINT2> tgPoints = null;
        //ErrorLogger.LogMessage("MultiPointHandler", "RenderSymbol()", "enter RenderSymbol", Level.FINER);
        
        String[] coordinates = controlPoints.split(" ");
        TGLight tgl = new TGLight();
        ArrayList<ShapeInfo> shapes = new ArrayList<ShapeInfo>();
        ArrayList<ShapeInfo> modifiers = new ArrayList<ShapeInfo>();
        //ArrayList<Point2D.Double> pixels = new ArrayList<Point2D.Double>();
        ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
        int len = coordinates.length;

        IPointConversion ipc = null;

        //Deutch moved section 6-29-11
        Double left = 0.0;
        Double right = 0.0;
        Double top = 0.0;
        Double bottom = 0.0;
        Point2D temp = null;
        Point2D ptGeoUL=null;
        int width=0;
        int height=0;
        int leftX = 0;
        int topY = 0;
        int bottomY = 0;
        int rightX = 0;
        int j=0;
        ArrayList<Point2D.Double> bboxCoords = null;
        if (bbox != null && bbox.equals("") == false) {            
            //ArrayList<Point2D> bboxCoords = null;
            String[] bounds = null;
            if(bbox.contains(" "))//trapezoid
            {
                bboxCoords = new ArrayList<Point2D.Double>();
                double x = 0;
                double y = 0;
                String[] coords = bbox.split(" ");
                String[] arrCoord;
                for(String coord : coords)
                {
                    arrCoord = coord.split(",");
                    x = Double.valueOf(arrCoord[0]);
                    y = Double.valueOf(arrCoord[1]);
                    bboxCoords.add(new Point2D.Double(x, y));
                }
                //use the upper left corner of the MBR containing geoCoords
                //to set the converter
                ptGeoUL=getGeoUL(bboxCoords);
                left=ptGeoUL.getX();
                top=ptGeoUL.getY();
                ipc = new PointConverter(left, top, scale);
                Point2D ptPixels=null;
                Point2D ptGeo=null;                
                for(j=0;j<bboxCoords.size();j++)
                {
                    ptGeo=bboxCoords.get(j);
                    ptPixels=ipc.GeoToPixels(ptGeo);
                    //diagnostic    12-27-12
                    x=ptPixels.getX();
                    y=ptPixels.getY();
                    if(x<20)
                        x=20;
                    if(y<20)
                        y=20;
                    ptPixels.setLocation(x, y);
                    //end section
                    bboxCoords.set(j, (Point2D.Double)ptPixels);
                }
            }
            else//rectangle
            {
                bounds = bbox.split(",");
                left = Double.valueOf(bounds[0]).doubleValue();
                right = Double.valueOf(bounds[2]).doubleValue();
                top = Double.valueOf(bounds[3]).doubleValue();
                bottom = Double.valueOf(bounds[1]).doubleValue();
                ipc = new PointConverter(left, top, scale);
            }

            // move these inside the bbox block above
            //left = Double.valueOf(bounds[0]).doubleValue();
            //right = Double.valueOf(bounds[2]).doubleValue();
            //top = Double.valueOf(bounds[3]).doubleValue();
            //bottom = Double.valueOf(bounds[1]).doubleValue();

            //added 2 lines Deutch 6-29-11
            //controlLong = left;
            //controlLat = top;
            //end section

            /*//old conversion
            int leftX = (int) GeoPixelConversion.long2x(left, scale, controlLong, top, metPerPix);
            int rightX = (int) GeoPixelConversion.long2x(right, scale, controlLong, top, metPerPix);

            int topY = (int) GeoPixelConversion.lat2y(top, scale, controlLat, metPerPix);
            int bottomY = (int) GeoPixelConversion.lat2y(bottom, scale, controlLat, metPerPix);
            //*/

            //new conversion
            //M. Deutch 11-29-12
            //TODO: swap two lines below when ready for coordinate update
            //ipc = new PointConverter(left, top, scale);
            //ipc = new PointConverter(left, top, right, bottom, scale);
            if(bboxCoords==null)
            {
                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();

                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                //////////////////

                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);

                rect = new Rectangle(leftX, topY, width, height);
            }
        } 
        else 
        {
            rect = null;
        }
        //end section

        for (int i = 0; i < len; i++) 
        {
            String[] coordPair = coordinates[i].split(",");
            Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
            Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
            geoCoords.add(new Point2D.Double(longitude, latitude));
        }
        if(ipc==null)
        {
            Point2D ptCoordsUL=getGeoUL(geoCoords);            
            ipc = new PointConverter(ptCoordsUL.getX(), ptCoordsUL.getY(), scale);            
        }
        //M. Deutch 9-26-11
        //rect=null;
        if(crossesIDL(geoCoords)==true)
            normalize=true;
        else
            normalize=false;

        //seems to work ok at world view
        if(normalize)
        {
            NormalizeGECoordsToGEExtents(0,360,geoCoords);
        }

        //commented out by deutch 5/22/2012
        /*
        //use an ipc based on the geocoords because of normalizing, not on the bbox
        Point2D pt2d=getControlPoint(geoCoords);
        controlLong=pt2d.getX();
        controlLat=pt2d.getY();
        //if(controlLong>left)
            ipc = new PointConverter(controlLong, controlLat, scale);
        //*////////////////////////////////
        
        //M. Deutch 10-3-11
        //must shift the rect pixels to synch with the new ipc
        //the old ipc was in synch with the bbox, so rect x,y was always 0,0
        //the new ipc synchs with the upper left of the geocoords so the boox is shifted
        //and therefore the clipping rectangle must shift by the delta x,y between
        //the upper left corner of the original bbox and the upper left corner of the geocoords

        ArrayList<Point2D.Double> geoCoords2 = new ArrayList<Point2D.Double>();
        geoCoords2.add(new Point2D.Double(left,top));
        geoCoords2.add(new Point2D.Double(right,bottom));

        if(normalize)
            NormalizeGECoordsToGEExtents(0,360,geoCoords2);

        //disable clipping
        if(ShouldClipSymbol(symbolCode)==false)
            rect = null;//disable clipping

        tgl.set_SymbolId(symbolCode);// "GFGPSLA---****X" AMBUSH symbol code
        tgl.set_Pixels(null);

        try {

            //String fillColor = null;
            MilStdSymbol mSymbol = new MilStdSymbol(symbolCode, null, geoCoords, null);

            //set milstd symbology standard.
            mSymbol.setSymbologyStandard(symStd);
            
            if(symbolModifiers != null && symbolModifiers.equals("")==false)
            {
                populateModifiers(symbolModifiers, mSymbol);
            }
            else
                mSymbol.setFillColor(null);

            //RendererSettings.getInstance().setTextRenderMethod(RendererSettings.TextBackgroundMethod_NONE);
            
            //get pixel values in case we need to do a fill.
            if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) || 
                    mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
            {
                tgl = clsRenderer.createTGLightFromMilStdSymbol(mSymbol, ipc);
                tgPoints = tgl.get_Pixels();
            }
            
            //old interface
//          clsRenderer.render_GE(tgl, shapes, modifiers, ipc, rect);
            
            //new interface
            IMultiPointRenderer mpr = MultiPointRenderer.getInstance();
            if(bboxCoords==null)
                mpr.renderWithPolylines(mSymbol, ipc, rect);
            else
                mpr.renderWithPolylines(mSymbol, ipc, bboxCoords);
            
            shapes = mSymbol.getSymbolShapes();
            modifiers = mSymbol.getModifierShapes();

            if (format == 1) {
                jsonOutput.append("{\"type\":\"symbol\",");
                jsonContent = JSONize(shapes, modifiers, ipc, true, normalize);
                jsonOutput.append(jsonContent);
                jsonOutput.append("}");
            } else if (format == 0) {

                jsonContent = KMLize(id, name, description, symbolCode, shapes, modifiers, ipc, true, normalize);
                
                //if there's a symbol fill or line pattern, add to KML//////////
                if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) || 
                        mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
                {
                    String fillKML = AddImageFillToKML(tgPoints, jsonContent, mSymbol, ipc, normalize);
                    if(fillKML != null && fillKML.equals("")==false)
                    {
                        jsonContent = fillKML;
                    }
                    
                    // <editor-fold defaultstate="collapsed" desc="code moved to AddImageFillToKML">
                    /*
                    //get original point values in pixel form                    
                    ArrayList<Point2D> pixelPoints = new ArrayList<Point2D>();
                    Path2D path = new Path2D.Double();
                    
                    //for(JavaLineArray.POINT2 pt : tgPoints)
                    int kcount = tgPoints.size();
                    JavaLineArray.POINT2 tpTemp = null;
                    for(int k = 0; k < kcount;k++)
                    {
                        tpTemp = tgPoints.get(k);
                        pixelPoints.add(new Point2D.Double(tpTemp.x, tpTemp.y));
                        if(k>0)
                        {
                            path.lineTo(tpTemp.x, tpTemp.y);
                        }
                        else
                        {
                            path.moveTo(tpTemp.x, tpTemp.y);
                        }
                    }
                    rect = path.getBounds();
                    //get url for the fill or line pattern PNG
                    String goImageUrl = SECWebRenderer.GenerateSymbolLineFillUrl(mSymbol.getModifierMap(), pixelPoints,rect);
                    //generate the extra KML needed to insert the image
                    String goKML = GenerateGroundOverlayKML(goImageUrl,ipc,rect,normalize);
                    goKML += "</Folder>";

                    //StringBuilder sb = new StringBuilder();
                    //sb.replace(start, end, str)
                    jsonContent = jsonContent.replace("</Folder>", goKML);//*/
                    // </editor-fold>
                    
                }///end if symbol fill or line pattern//////////////////////////

                jsonOutput.append(jsonContent);
            }

        } catch (Exception exc) {
            String st = JavaRendererUtilities.getStackTrace(exc);
            jsonOutput = new StringBuilder();
            jsonOutput.append("{\"type\":\"error\",\"error\":\"There was an error creating the MilStdSymbol " + symbolCode + ": " + "- ");
            jsonOutput.append(exc.getMessage() + " - ");
            jsonOutput.append(st);
            jsonOutput.append("\"}");
            
            //ErrorLogger.LogException("MultiPointHandler", "RenderSymbol", exc);
        }


        debug = false;
        if (debug == true) {
            System.out.println("Symbol Code: " + symbolCode);
            System.out.println("Scale: " + scale);
            System.out.println("BBOX: " + bbox);
            if (controlPoints != null) {
                System.out.println("Geo Points: " + controlPoints);
            }
            if (tgl != null && tgl.get_Pixels() != null)//pixels != null
            {
                //System.out.println("Pixel: " + pixels.toString());
                System.out.println("Pixel: " + tgl.get_Pixels().toString());
            }
            if (bbox != null) {
                System.out.println("geo bounds: " + bbox);
            }
            if (rect != null) {
                System.out.println("pixel bounds: " + rect.toString());
            }
            if (jsonOutput != null) {
                System.out.println(jsonOutput.toString());
            }
        }

        //ErrorLogger.LogMessage("MultiPointHandler", "RenderSymbol()", "exit RenderSymbol", Level.FINER);
        return jsonOutput.toString();

    }
    
    /**
     * 
     * @param tgPoints
     * @param jsonContent
     * @param mSymbol
     * @param ipc
     * @param normalize 
     */
    static private String AddImageFillToKML(ArrayList<JavaLineArray.POINT2> tgPoints,
            String jsonContent, MilStdSymbol mSymbol, IPointConversion ipc, Boolean normalize)
    {
        //get original point values in pixel form                    
        ArrayList<Point2D> pixelPoints = new ArrayList<Point2D>();
        Path2D path = new Path2D.Double();

        //for(JavaLineArray.POINT2 pt : tgPoints)
        int kcount = tgPoints.size();
        JavaLineArray.POINT2 tpTemp = null;
        for(int k = 0; k < kcount;k++)
        {
            tpTemp = tgPoints.get(k);
            pixelPoints.add(new Point2D.Double(tpTemp.x, tpTemp.y));
            if(k>0)
            {
                path.lineTo(tpTemp.x, tpTemp.y);
            }
            else
            {
                path.moveTo(tpTemp.x, tpTemp.y);
            }
        }
        Rectangle rect = path.getBounds();
        //get url for the fill or line pattern PNG
        String goImageUrl = SECWebRenderer.GenerateSymbolLineFillUrl(mSymbol.getModifierMap(), pixelPoints,rect);
        //generate the extra KML needed to insert the image
        String goKML = GenerateGroundOverlayKML(goImageUrl,ipc,rect,normalize);
        goKML += "</Folder>";

        //StringBuilder sb = new StringBuilder();
        //sb.replace(start, end, str)
        jsonContent = jsonContent.replace("</Folder>", goKML);
        
        return jsonContent;
    }
    
     static public PNGInfo GenerateImageFillPNGInfo(ArrayList<JavaLineArray.POINT2> tgPoints,
            String jsonContent, MilStdSymbol mSymbol, IPointConversion ipc, Boolean normalize)
    {
        //get original point values in pixel form                    
        ArrayList<Point2D> pixelPoints = new ArrayList<Point2D>();
        Path2D path = new Path2D.Double();

        //for(JavaLineArray.POINT2 pt : tgPoints)
        int kcount = tgPoints.size();
        JavaLineArray.POINT2 tpTemp = null;
        for(int k = 0; k < kcount;k++)
        {
            tpTemp = tgPoints.get(k);
            pixelPoints.add(new Point2D.Double(tpTemp.x, tpTemp.y));
            if(k>0)
            {
                path.lineTo(tpTemp.x, tpTemp.y);
            }
            else
            {
                path.moveTo(tpTemp.x, tpTemp.y);
            }
        }
        Rectangle rect = path.getBounds();
        double centerX = rect.getCenterX();
        double centerY = rect.getCenterY();
        //get url for the fill or line pattern PNG
        String goImageUrl = SECWebRenderer.GenerateSymbolLineFillUrl(mSymbol.getModifierMap(), pixelPoints,rect);
        
        //System.out.println("fillURL: " + goImageUrl);
        
        Map<String,String> params = SinglePointRendererService.getInstance().processParams(goImageUrl);
        
        //System.out.println("params:");
        //System.out.println(ErrorLogger.PrintStringMap(params));
        
        ISinglePointInfo spi = SinglePointRendererService.getInstance().render(params.get("RENDERER"), "AREASYMBOLFILL", params);
        PNGInfo pi = new PNGInfo(spi);

        //get extents for image fill////////////////////////////////////////////
        Point2D topLeft = null;
        Point2D bottomRight = null;
        Rectangle2D bounds = path.getBounds2D();
        double imageSize = 0;
        if(params.containsKey(AreaSymbolFill.KEY_SYMBOL_FILL_ICON_SIZE))
        {
            imageSize = Double.valueOf(params.get(AreaSymbolFill.KEY_SYMBOL_FILL_ICON_SIZE));
        }
        else
        {
            imageSize = (double)AreaSymbolFill.DEFAULT_SYMBOL_SIZE;
        }
        double imageOffset = 0;
        if(params.containsKey(AreaSymbolFill.KEY_SYMBOL_LINE_IDS))
        {
            imageOffset = (imageSize/2) +3;//+3 to make room for rotation
        }

        double height, width, x, y;
        
        height = bounds.getHeight()+(imageOffset*2);
                    width = bounds.getWidth()+(imageOffset*2);
                    x = bounds.getX()-imageOffset;
                    y = bounds.getY()-imageOffset;

        Point2D coord = (Point2D) new Point2D.Double(x, y);
                    topLeft = ipc.PixelsToGeo(coord);
                    coord = (Point2D) new Point2D.Double(x+width,y+height);
                    bottomRight = ipc.PixelsToGeo(coord);
        
        if(normalize)
        {
            topLeft=NormalizeCoordToGECoord(topLeft);
            bottomRight=NormalizeCoordToGECoord(bottomRight);
        }
        
        pi = new PNGInfo(spi.getImage(), topLeft, new Rectangle2D.Double(
                topLeft.getX(), topLeft.getY(), bottomRight.getX(), bottomRight.getY()));
        
       
        return pi;

    }

        /**
     * Deutch updated version
     * @param id
     * @param name
     * @param description
     * @param symbolCode
     * @param controlPoints
     * @param scale
     * @param bbox
     * @param shapes
     * @param symStd 0=2525Bch2, 1=2525C
     */
    @SuppressWarnings("deprecation")
	public static MilStdSymbol RenderSymbolAsMilStdSymbol(String id,
            String name,
            String description,
            String symbolCode,
            String controlPoints,
            Double scale,
            String bbox,
            String symbolModifiers,
            int symStd)//,
    //ArrayList<ShapeInfo>shapes)
    {
        MilStdSymbol mSymbol = null;
        //System.out.println("MultiPointHandler.RenderSymbol()");
        boolean normalize = false;
        Double controlLat = 0.0;
        Double controlLong = 0.0;
        //Double metPerPix = GeoPixelConversion.metersPerPixel(scale);
        //String bbox2=getBoundingRectangle(controlPoints,bbox);
        StringBuilder jsonOutput = new StringBuilder();
        String jsonContent = "";

        Rectangle rect = null;

        //for symbol & line fill
        ArrayList<JavaLineArray.POINT2> tgPoints = null;
        //ErrorLogger.LogMessage("MultiPointHandler", "RenderSymbol()", "enter RenderSymbol", Level.FINER);
        
        String[] coordinates = controlPoints.split(" ");
        TGLight tgl = new TGLight();
        ArrayList<ShapeInfo> shapes = new ArrayList<ShapeInfo>();
        ArrayList<ShapeInfo> modifiers = new ArrayList<ShapeInfo>();
        //ArrayList<Point2D.Double> pixels = new ArrayList<Point2D.Double>();
        ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
        int len = coordinates.length;

        IPointConversion ipc = null;

        //Deutch moved section 6-29-11
        Double left = 0.0;
        Double right = 0.0;
        Double top = 0.0;
        Double bottom = 0.0;
        Point2D temp = null;
        int width=0;
        int height=0;
        int leftX = 0;
        int topY = 0;
        int bottomY = 0;
        int rightX = 0;
        ArrayList<Point2D.Double> bboxCoords = null;

        if (bbox != null && bbox.equals("") == false) 
        {
            //System.out.println(bbox);
            if(bbox.contains(" "))//trapezoid
            {   //System.out.println("trapezoid");
                
                bboxCoords = new ArrayList<Point2D.Double>();
                double x = 0;
                double y = 0;
                String[] coords = bbox.split(" ");
                String[] arrCoord;
                for(String coord : coords)
                {
                    arrCoord = coord.split(",");
                    x = Double.valueOf(arrCoord[0]);
                    y = Double.valueOf(arrCoord[1]);
                    bboxCoords.add(new Point2D.Double(x, y));
                }
                //use the upper left corner of the MBR containing geoCoords
                //to set the converter
                Point2D ptGeoUL=getGeoUL(bboxCoords);
                double ptLeft=ptGeoUL.getX();
                double ptTop=ptGeoUL.getY();
                ipc = new PointConverter(ptLeft, ptTop, scale);
                Point2D ptPixels=null;
                Point2D ptGeo=null;                
                for(int j=0;j<bboxCoords.size();j++)
                {
                    ptGeo=bboxCoords.get(j);
                    ptPixels=ipc.GeoToPixels(ptGeo);
                    //diagnostic    12-27-12
                    x=ptPixels.getX();
                    y=ptPixels.getY();
                    if(x<20)
                        x=20;
                    if(y<20)
                        y=20;
                    ptPixels.setLocation(x, y);
                    //end section
                    bboxCoords.set(j, (Point2D.Double)ptPixels);
                }
            }
            else//rectangle
            {   //System.out.println("rect");
                String[] bounds = bbox.split(",");

                left = Double.valueOf(bounds[0]).doubleValue();
                right = Double.valueOf(bounds[2]).doubleValue();
                top = Double.valueOf(bounds[3]).doubleValue();
                bottom = Double.valueOf(bounds[1]).doubleValue();

                //added 2 lines Deutch 6-29-11
                controlLong = left;
                controlLat = top;
                //end section


                //new conversion
                //M. Deutch 11-29-12
                //TODO: swap two lines below when ready for coordinate update
                ipc = new PointConverter(controlLong, controlLat, scale);
                //ipc = new PointConverter(left, top, right, bottom, scale);

                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();

                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                //////////////////

                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);

                rect = new Rectangle(leftX, topY, width, height);
            }
        } 
        else 
        {
            rect = null;
        }
        //end section

        for (int i = 0; i < len; i++) {
            String[] coordPair = coordinates[i].split(",");
            Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
            Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
            geoCoords.add(new Point2D.Double(longitude, latitude));
        }
        if(ipc==null)
        {
            Point2D ptCoordsUL=getGeoUL(geoCoords);            
            ipc = new PointConverter(ptCoordsUL.getX(), ptCoordsUL.getY(), scale);            
        }
        //M. Deutch 9-26-11
        //rect=null;
        if(crossesIDL(geoCoords)==true)
            normalize=true;
        else
            normalize=false;


        //seems to work ok at world view
        if(normalize)
        {
            NormalizeGECoordsToGEExtents(0,360,geoCoords);
        }

       
        //M. Deutch 10-3-11
        //must shift the rect pixels to synch with the new ipc
        //the old ipc was in synch with the bbox, so rect x,y was always 0,0
        //the new ipc synchs with the upper left of the geocoords so the boox is shifted
        //and therefore the clipping rectangle must shift by the delta x,y between
        //the upper left corner of the original bbox and the upper left corner of the geocoords

        ArrayList<Point2D.Double> geoCoords2 = new ArrayList<Point2D.Double>();
        geoCoords2.add(new Point2D.Double(left,top));
        geoCoords2.add(new Point2D.Double(right,bottom));

        if(normalize)
            NormalizeGECoordsToGEExtents(0,360,geoCoords2);


        if(ShouldClipSymbol(symbolCode)==false)
            rect = null;//disable clipping

        tgl.set_SymbolId(symbolCode);// "GFGPSLA---****X" AMBUSH symbol code
        tgl.set_Pixels(null);

        try {

            String fillColor = null;
            mSymbol = new MilStdSymbol(symbolCode, null, geoCoords, null);

            //set milstd symbology standard.
            mSymbol.setSymbologyStandard(symStd);
            
            if(symbolModifiers != null && symbolModifiers.equals("")==false)
            {
                populateModifiers(symbolModifiers, mSymbol);
            }
            else
                mSymbol.setFillColor(null);

            if(mSymbol.getFillColor() != null)
            {
                Color fc = mSymbol.getFillColor();
                fillColor = Integer.toHexString(fc.getRGB());                
            }

            //get pixel values in case we need to do a fill.
            if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) || 
                    mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
            {
                tgl = clsRenderer.createTGLightFromMilStdSymbol(mSymbol, ipc);
                tgPoints = tgl.get_Pixels();
            }
//            clsRenderer.render_GE(tgl, shapes, modifiers, ipc, rect);
            //new interface
            IMultiPointRenderer mpr = MultiPointRenderer.getInstance();
            if(bboxCoords==null)
                mpr.renderWithPolylines(mSymbol, ipc, rect);
            else
                mpr.renderWithPolylines(mSymbol, ipc, bboxCoords);
            
            shapes = mSymbol.getSymbolShapes();
            modifiers = mSymbol.getModifierShapes();
            
            //convert points////////////////////////////////////////////////////
            ArrayList<ArrayList<Point2D>> polylines = null;
            ArrayList<ArrayList<Point2D>> newPolylines = null;
            ArrayList<Point2D> newLine = null;
            for(ShapeInfo shape : shapes)
            {
                polylines = shape.getPolylines();
                //System.out.println("pixel polylines: " + String.valueOf(polylines));
                newPolylines = ConvertPolylinePixelsToCoords(polylines, ipc,normalize);
                shape.setPolylines(newPolylines);
            }
            
            for(ShapeInfo label : modifiers)
            {
                Point2D pixelCoord = label.getModifierStringPosition();
                if(pixelCoord==null)
                {
                    pixelCoord = label.getGlyphPosition();
                }
                Point2D geoCoord = ipc.PixelsToGeo(pixelCoord);

                if(normalize)
                {
                    geoCoord=NormalizeCoordToGECoord(geoCoord);
                }

                double latitude = geoCoord.getY();
                double longitude = geoCoord.getX();
                label.setModifierStringPosition(new Point2D.Double(longitude, latitude));
                
                //get angle and assign to glyph position.
                //if(label.getModifierStringAngle() != 0.0)
                //{
                    Point2D end = JavaRendererUtilities.getEndPointWithAngle(pixelCoord, label.getModifierStringAngle(), 10.0/*label.getTextLayout().getBounds().getWidth()*/);
                    Point2D geoAngle = ipc.PixelsToGeo(end);
                    if(normalize)
                    {
                        geoAngle = NormalizeCoordToGECoord(geoAngle);
                    }
                    label.setGlyphPosition(geoAngle);//*/
            }   //}
           
            ////////////////////////////////////////////////////////////////////
            mSymbol.setModifierShapes(modifiers);
            mSymbol.setSymbolShapes(shapes);
            
            //Create Image Fill
            if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) ||
                    mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
            {
                PNGInfo pi = GenerateImageFillPNGInfo(tgPoints, jsonContent, mSymbol, ipc, normalize);
                
                
                
                Rectangle2D bounds = null;
                
                mSymbol.setTag(pi);
            }
            

        } catch (Exception exc) {
            jsonOutput = new StringBuilder();
            jsonOutput.append("{\"type\":\"error\",\"error\":\"There was an error creating the MilStdSymbol " + symbolCode + ": " + "- ");
            jsonOutput.append(exc.getMessage() + " - ");
            //jsonOutput.append(ErrorLogger.getStackTrace(exc));
            jsonOutput.append("\"}");
            
            System.out.println(exc.getMessage());
            exc.printStackTrace();
        }


        boolean debug = false;
        if (debug == true) {
            System.out.println("Symbol Code: " + symbolCode);
            System.out.println("Scale: " + scale);
            System.out.println("BBOX: " + bbox);
            if (controlPoints != null) {
                System.out.println("Geo Points: " + controlPoints);
            }
            if (tgl != null && tgl.get_Pixels() != null)//pixels != null
            {
                //System.out.println("Pixel: " + pixels.toString());
                System.out.println("Pixel: " + tgl.get_Pixels().toString());
            }
            if (bbox != null) {
                System.out.println("geo bounds: " + bbox);
            }
            if (rect != null) {
                System.out.println("pixel bounds: " + rect.toString());
            }
            if (jsonOutput != null) {
                System.out.println(jsonOutput.toString());
            }
        }

        //ErrorLogger.LogMessage("MultiPointHandler", "RenderSymbolAsMilStdSymbol()", "exit RenderSymbol", Level.FINER);
        return mSymbol;

    }
    
    private static ArrayList<ArrayList<Point2D>> ConvertPolylinePixelsToCoords(ArrayList<ArrayList<Point2D>> polylines, IPointConversion ipc, Boolean normalize)
    {
        ArrayList<ArrayList<Point2D>> newPolylines = new ArrayList<ArrayList<Point2D>>();

        double latitude = 0;
        double longitude = 0;
        ArrayList<Point2D> newLine = null;
        try
        {
            for(ArrayList<Point2D> line : polylines)
            {
                newLine = new ArrayList<Point2D>();
                for(Point2D pt : line)
                {
                    Point2D geoCoord = ipc.PixelsToGeo(pt);

                    if(normalize)
                    {
                        geoCoord=NormalizeCoordToGECoord(geoCoord);
                    }

                    latitude = geoCoord.getY();
                    longitude = geoCoord.getX();
                    newLine.add(new Point2D.Double(longitude, latitude));
                }
                newPolylines.add(newLine);
            }
        }
        catch(Exception exc)
        {
            System.out.println(exc.getMessage());
            exc.printStackTrace();
        }
        return newPolylines;
    }
    
    /**
     * Multipoint Rendering on flat 2D maps
     * @param id A unique ID for the symbol.  only used in KML currently
     * @param symbolCode
     * @param controlPoints
     * @param pixelWidth pixel dimensions of the viewable map area
     * @param pixelHeight pixel dimensions of the viewable map area
     * @param bbox The viewable area of the map.  Passed in the format of a
     * string "lowerLeftX,lowerLeftY,upperRightX,upperRightY."
     * example: "-50.4,23.6,-42.2,24.2"
     * @param symbolModifiers A JSON string representing all the possible symbol
     * modifiers represented in the MIL-STD-2525C.  Format of the string will be
     * {"modifiers": {"attributeName":"value"[,"attributeNamen":"valuen"]...}}
     * The quotes are literal in the above notation.  Example:
     * {"modifiers": {"quantity":"4","speed":"300","azimuth":[100,200]}}
     * @param format An enumeration: 0 for KML, 1 for JSON.
     * @return A JSON or KML string representation of the graphic.
     */
    public static String RenderSymbol2D(String id, 
            String name, 
            String description, 
            String symbolCode,
            String controlPoints,
            int pixelWidth,
            int pixelHeight,
            String bbox,
            String symbolModifiers,
            int format)
    {
        return RenderSymbol2D(id, name, description, symbolCode, controlPoints, 
                pixelWidth, pixelHeight, bbox, symbolModifiers, format, 
                RendererSettings.getInstance().getSymbologyStandard());
    }
    
    /**
     * Multipoint Rendering on flat 2D maps
     * @param id A unique ID for the symbol.  only used in KML currently
     * @param symbolCode
     * @param controlPoints
     * @param pixelWidth pixel dimensions of the viewable map area
     * @param pixelHeight pixel dimensions of the viewable map area
     * @param bbox The viewable area of the map.  Passed in the format of a
     * string "lowerLeftX,lowerLeftY,upperRightX,upperRightY."
     * example: "-50.4,23.6,-42.2,24.2"
     * @param symbolModifiers A JSON string representing all the possible symbol
     * modifiers represented in the MIL-STD-2525C.  Format of the string will be
     * {"modifiers": {"attributeName":"value"[,"attributeNamen":"valuen"]...}}
     * The quotes are literal in the above notation.  Example:
     * {"modifiers": {"quantity":"4","speed":"300","azimuth":[100,200]}}
     * @param format An enumeration: 0 for KML, 1 for JSON.
     * @param symStd An enumeration: 0 for 2525Bch2, 1 for 2525C.
     * @return A JSON or KML string representation of the graphic.
     */
    public static String RenderSymbol2D(String id, 
            String name, 
            String description, 
            String symbolCode,
            String controlPoints,
            int pixelWidth,
            int pixelHeight,
            String bbox,
            String symbolModifiers,
            int format, int symStd)//,
    //ArrayList<ShapeInfo>shapes)
    {

        
        StringBuilder jsonOutput = new StringBuilder();
        String jsonContent = "";

        Rectangle rect = null;

        ArrayList<JavaLineArray.POINT2> tgPoints = null;

        String[] coordinates = controlPoints.split(" ");
        TGLight tgl = new TGLight();
        ArrayList<ShapeInfo> shapes = new ArrayList<ShapeInfo>();
        ArrayList<ShapeInfo> modifiers = new ArrayList<ShapeInfo>();
        ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
        IPointConversion ipc = null;


        //Deutch moved section 6-29-11
        Double left = 0.0;
        Double right = 0.0;
        Double top = 0.0;
        Double bottom = 0.0;
        if (bbox != null && bbox.equals("") == false) {
            String[] bounds = bbox.split(",");

            left = Double.valueOf(bounds[0]).doubleValue();
            right = Double.valueOf(bounds[2]).doubleValue();
            top = Double.valueOf(bounds[3]).doubleValue();
            bottom = Double.valueOf(bounds[1]).doubleValue();

            ipc = new PointConversion(pixelWidth, pixelHeight, top, left, bottom, right);
        } else {
            System.out.println("Bad bbox value: " + bbox);
            System.out.println("bbox is viewable area of the map.  Passed in the format of a string \"lowerLeftX,lowerLeftY,upperRightX,upperRightY.\" example: \"-50.4,23.6,-42.2,24.2\"");
            return "ERROR - Bad bbox value: " + bbox;
        }
        //end section

        //get coordinates
        int len = coordinates.length;
        for (int i = 0; i < len; i++) {
            String[] coordPair = coordinates[i].split(",");
            Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
            Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
            geoCoords.add(new Point2D.Double(longitude, latitude));
        }

        try {
            MilStdSymbol mSymbol = new MilStdSymbol(symbolCode, null, geoCoords, null);
            
            //set milstd symbology standard.
            mSymbol.setSymbologyStandard(symStd);

            if (symbolModifiers != null && symbolModifiers.equals("") == false) {
                populateModifiers(symbolModifiers, mSymbol);
            }
            else
                mSymbol.setFillColor(null);
            
            //build clipping bounds
            Point2D temp = null;
            int leftX;
            int topY;
            int bottomY;
            int rightX;
            int width;
            int height;
            if(ShouldClipSymbol(symbolCode))
            {
                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();

                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                //////////////////

                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);

                rect = new Rectangle(leftX, topY, width, height);
            }

            
            if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) || 
                        mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
            {
                tgl = clsRenderer.createTGLightFromMilStdSymbol(mSymbol, ipc);
                tgPoints = tgl.get_Pixels();
            }
            
            //new interface
            IMultiPointRenderer mpr = MultiPointRenderer.getInstance();
            mpr.renderWithPolylines(mSymbol, ipc, rect);
            shapes = mSymbol.getSymbolShapes();
            modifiers = mSymbol.getModifierShapes();

            boolean normalize = false;

            if (format == 1) {
                jsonOutput.append("{\"type\":\"symbol\",");
                jsonContent = JSONize(shapes, modifiers, ipc, false, normalize);
                jsonOutput.append(jsonContent);
                jsonOutput.append("}");
            } else if (format == 0) {
                String fillColor = null;
                if(mSymbol.getFillColor() != null)
                    fillColor = Integer.toHexString(mSymbol.getFillColor().getRGB());//Integer.toHexString(shapeInfo.getFillColor().getRGB()
                jsonContent = KMLize(id, name, description, symbolCode, shapes, modifiers, ipc, false, normalize);
                
                //if there's a symbol fill or line pattern, add to KML//////////
                if(mSymbol.getModifierMap().containsKey(SYMBOL_FILL_IDS) || 
                        mSymbol.getModifierMap().containsKey(SYMBOL_LINE_IDS))
                {
                    String fillKML = AddImageFillToKML(tgPoints, jsonContent, mSymbol, ipc, normalize);
                    if(fillKML != null && fillKML.equals("")==false)
                    {
                        jsonContent = fillKML;
                    }
                }///end if symbol fill or line pattern//////////////////////////
                
                jsonOutput.append(jsonContent);
            }

        } catch (Exception exc) {
            jsonOutput = new StringBuilder();
            jsonOutput.append("{\"type\":\"error\",\"error\":\"There was an error creating the MilStdSymbol " + symbolCode + ": " + "- ");
            jsonOutput.append(exc.getMessage() + " - ");
            //jsonOutput.append(ErrorLogger.getStackTrace(exc));
            jsonOutput.append("\"}");
        }


        boolean debug = false;
        if (debug == true) {
            System.out.println("Symbol Code: " + symbolCode);
            System.out.println("BBOX: " + bbox);
            if (controlPoints != null) {
                System.out.println("Geo Points: " + controlPoints);
            }
            if (tgl != null && tgl.get_Pixels() != null)//pixels != null
            {
                //System.out.println("Pixel: " + pixels.toString());
                System.out.println("Pixel: " + tgl.get_Pixels().toString());
            }
            if (bbox != null) {
                System.out.println("geo bounds: " + bbox);
            }
            if (rect != null) {
                System.out.println("pixel bounds: " + rect.toString());
            }
            if (jsonOutput != null) {
                System.out.println(jsonOutput.toString());
            }
        }

        return jsonOutput.toString();

    }
    
    /**
     * For Mike Deutch testing
     * @param id
     * @param name
     * @param description
     * @param symbolCode
     * @param controlPoints
     * @param pixelWidth
     * @param pixelHeight
     * @param bbox
     * @param symbolModifiers
     * @param shapes
     * @param modifiers
     * @param format
     * @return 
     * @deprecated 
     */
    public static String RenderSymbol2DX(String id, 
            String name, 
            String description, 
            String symbolCode,
            String controlPoints,
            int pixelWidth,
            int pixelHeight,
            String bbox,
            String symbolModifiers,
            ArrayList<ShapeInfo> shapes,
            ArrayList<ShapeInfo> modifiers,
            int format)//,
    //ArrayList<ShapeInfo>shapes)
    {


        StringBuilder jsonOutput = new StringBuilder();
        String jsonContent = "";

        Rectangle rect = null;


        String[] coordinates = controlPoints.split(" ");
        TGLight tgl = new TGLight();
        //ArrayList<ShapeInfo> shapes = new ArrayList<ShapeInfo>();
        //ArrayList<ShapeInfo> modifiers = new ArrayList<ShapeInfo>();
        ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
        IPointConversion ipc = null;


        //Deutch moved section 6-29-11
        Double left = 0.0;
        Double right = 0.0;
        Double top = 0.0;
        Double bottom = 0.0;
        if (bbox != null && bbox.equals("") == false) {
            String[] bounds = bbox.split(",");

            left = Double.valueOf(bounds[0]).doubleValue();
            right = Double.valueOf(bounds[2]).doubleValue();
            top = Double.valueOf(bounds[3]).doubleValue();
            bottom = Double.valueOf(bounds[1]).doubleValue();

            ipc = new PointConversion(pixelWidth, pixelHeight, top, left, bottom, right);
        } else {
            System.out.println("Bad bbox value: " + bbox);
            System.out.println("bbox is viewable area of the map.  Passed in the format of a string \"lowerLeftX,lowerLeftY,upperRightX,upperRightY.\" example: \"-50.4,23.6,-42.2,24.2\"");
            return "ERROR - Bad bbox value: " + bbox;
        }
        //end section

        //get coordinates
        int len = coordinates.length;
        for (int i = 0; i < len; i++) {
            String[] coordPair = coordinates[i].split(",");
            Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
            Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
            geoCoords.add(new Point2D.Double(longitude, latitude));
        }

        try {
            MilStdSymbol mSymbol = new MilStdSymbol(symbolCode, null, geoCoords, null);

            if (symbolModifiers != null && symbolModifiers.equals("") == false) {
                populateModifiers(symbolModifiers, mSymbol);
            }
            else
                mSymbol.setFillColor(null);

            //RendererSettings.getInstance().setTextRenderMethod(RendererSettings.TextBackgroundMethod_NONE);
//            tgl = clsRenderer.createTGLightFromMilStdSymbol(mSymbol, ipc);
//            clsRenderer.render_GE(tgl, shapes, modifiers, ipc, rect);
            //new interface
            IMultiPointRenderer mpr = MultiPointRenderer.getInstance();
            mpr.renderWithPolylines(mSymbol, ipc, rect);
            shapes = mSymbol.getSymbolShapes();
            modifiers = mSymbol.getModifierShapes();

            boolean normalize = false;

            if (format == 1) {
                jsonOutput.append("{\"type\":\"symbol\",");
                jsonContent = JSONize(shapes, modifiers, ipc, false, normalize);
                jsonOutput.append(jsonContent);
                jsonOutput.append("}");
            } else if (format == 0) {
                String fillColor = null;
                if(mSymbol.getFillColor() != null)
                    fillColor = Integer.toHexString(mSymbol.getFillColor().getRGB());//Integer.toHexString(shapeInfo.getFillColor().getRGB()
                jsonContent = KMLize(id, name, description, symbolCode, shapes, modifiers, ipc, false, normalize);
                jsonOutput.append(jsonContent);
            }

        } catch (Exception exc) {
            jsonOutput = new StringBuilder();
            jsonOutput.append("{\"type\":\"error\",\"error\":\"There was an error creating the MilStdSymbol " + symbolCode + ": " + "- ");
            jsonOutput.append(exc.getMessage() + " - ");
            //jsonOutput.append(ErrorLogger.getStackTrace(exc));
            jsonOutput.append("\"}");
        }


        boolean debug = true;
        if (debug == true) {
            System.out.println("Symbol Code: " + symbolCode);
            System.out.println("BBOX: " + bbox);
            if (controlPoints != null) {
                System.out.println("Geo Points: " + controlPoints);
            }
            if (tgl != null && tgl.get_Pixels() != null)//pixels != null
            {
                //System.out.println("Pixel: " + pixels.toString());
                System.out.println("Pixel: " + tgl.get_Pixels().toString());
            }
            if (bbox != null) {
                System.out.println("geo bounds: " + bbox);
            }
            if (rect != null) {
                System.out.println("pixel bounds: " + rect.toString());
            }
            if (jsonOutput != null) {
                System.out.println(jsonOutput.toString());
            }
        }
        return jsonOutput.toString();

    }
    
    
    /**
     * 
     * @param symbolID
     * @param coordinates
     * @param scale
     * @param bboxTL
     * @param bboxBR
     * @param modifiers
     * @param X_Altitude
     * @param AM_Distance
     * @param AN_Azimuth
     * @return 
     * @deprecated 
     */
    public static SymbolInfo RenderSymbol3DWW(
            String symbolID,
            ArrayList<Point2D.Double> coordinates,
            Double scale,
            Point2D.Double bboxTL,
            Point2D.Double bboxBR,
            Map<String,String> modifiers,
            ArrayList<Double> X_Altitude,
            ArrayList<Double> AM_Distance,
            ArrayList<Double> AN_Azimuth)
    {
        try
        {
            MilStdSymbol symbol = new MilStdSymbol(symbolID,null,coordinates,modifiers);
            if(AM_Distance != null)
                symbol.setModifiers_AM_AN_X(ModifiersTG.AM_DISTANCE, AM_Distance);
            if(AN_Azimuth != null)
                symbol.setModifiers_AM_AN_X(ModifiersTG.AN_AZIMUTH, AN_Azimuth);
            if(X_Altitude != null)
                symbol.setModifiers_AM_AN_X(ModifiersTG.X_ALTITUDE_DEPTH, X_Altitude);
            
            symbol = RenderSymbol3DWW(symbol,scale,bboxTL,bboxBR);
            
            SymbolInfo si = MilStdSymbolToSymbolInfo(symbol);
            
            return si;
        }
        catch(Exception exc)
        {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
        }
        return null;
    }
    
    /**
     * 
     * @param symbol
     * @param scale
     * @param bboxTL
     * @param bboxBR
     * @return 
     * @deprecated
     */
    public static MilStdSymbol RenderSymbol3DWW(
            MilStdSymbol symbol,
            Double scale,
            Point2D.Double bboxTL,
            Point2D.Double bboxBR)//,
    //ArrayList<ShapeInfo>shapes)
    {
        try
        {
            boolean normalize = false;
            Double controlLat = 0.0;
            Double controlLong = 0.0;

            Rectangle2D rect = null;
            TGLight tgl = new TGLight();
            ArrayList<ShapeInfo> shapes = new ArrayList<ShapeInfo>();
            ArrayList<ShapeInfo> modifiers = new ArrayList<ShapeInfo>();
            //ArrayList<Point2D.Double> pixels = new ArrayList<Point2D.Double>();
            ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
            String symbolCode = symbol.getSymbolID();

            IPointConversion ipc = null;

            //Deutch moved section 6-29-11
            Double left = 0.0;
            Double right = 0.0;
            Double top = 0.0;
            Double bottom = 0.0;
            Point2D temp = null;
            int width=0;
            int height=0;
            int leftX = 0;
            int topY = 0;
            int bottomY = 0;
            int rightX = 0;
            if (bboxTL != null && bboxBR != null) {


                left = bboxTL.getX();
                right = bboxBR.getX();
                top = bboxTL.getY();
                bottom = bboxBR.getY();

                //added 2 lines Deutch 6-29-11
                controlLong = left;
                controlLat = top;
                //end section


                //new conversion
                ipc = new PointConverter(controlLong, controlLat, scale);

                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();

                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                //////////////////

                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);

                rect = new Rectangle(leftX, topY, width, height);
            } else {
                rect = null;
            }
            //end section

            geoCoords = symbol.getCoordinates();

            //M. Deutch 9-26-11
            //rect=null;
            if(crossesIDL(geoCoords)==true)
                normalize=true;
            else
                normalize=false;

            //seems to work ok at world view
            if(normalize)
            {
                NormalizeGECoordsToGEExtents(0,360,geoCoords);
            }

            //use an ipc based on the geocoords because of normalizing, not on the bbox
            Point2D pt2d=getControlPoint(geoCoords);
            controlLong=pt2d.getX();
            controlLat=pt2d.getY();
            //if(controlLong>left)
                ipc = new PointConverter(controlLong, controlLat, scale);

            //M. Deutch 10-3-11
            //must shift the rect pixels to synch with the new ipc
            //the old ipc was in synch with the bbox, so rect x,y was always 0,0
            //the new ipc synchs with the upper left of the geocoords so the boox is shifted
            //and therefore the clipping rectangle must shift by the delta x,y between
            //the upper left corner of the original bbox and the upper left corner of the geocoords

            ArrayList<Point2D.Double> geoCoords2 = new ArrayList<Point2D.Double>();
            geoCoords2.add(new Point2D.Double(left,top));
            geoCoords2.add(new Point2D.Double(right,bottom));

            if(normalize)
                NormalizeGECoordsToGEExtents(0,360,geoCoords2);

            if(rect != null)
            {
                left=geoCoords2.get(0).getX();
                top=geoCoords2.get(0).getY();
                //M. Deutch 10-5-11 added on line
                //ipc = new PointConverter(left, top, scale);
                right=geoCoords2.get(1).getX();
                bottom=geoCoords2.get(1).getY();
                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();
                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);
                rect = new Rectangle(leftX, topY, width, height);
            }
            //disable clipping
            if(ShouldClipSymbol(symbolCode)==false)
                rect = null;//disable clipping

            tgl.set_SymbolId(symbolCode);// "GFGPSLA---****X" AMBUSH symbol code
            tgl.set_Pixels(null);

            try {

                symbol.setCoordinates(geoCoords);

                //RendererSettings.getInstance().setTextRenderMethod(RendererSettings.TextBackgroundMethod_NONE);
//                tgl = clsRenderer.createTGLightFromMilStdSymbol(symbol, ipc);
//                clsRenderer.render_GE(tgl, shapes, modifiers, ipc, rect);
                //new interface
                IMultiPointRenderer mpr = MultiPointRenderer.getInstance();
                mpr.renderWithPolylines(symbol, ipc, rect);
                shapes = symbol.getSymbolShapes();
                modifiers = symbol.getModifierShapes();    


                MakeWWReady(shapes, modifiers, ipc, normalize);

                symbol.setSymbolShapes(shapes);
                symbol.setModifierShapes(modifiers);

                return symbol;


            } 
            catch (Exception exc) 
            {
                System.err.println(exc.getMessage());
                exc.printStackTrace();
            }


            boolean debug = false;
            if (debug == true) {
                System.out.println("Symbol Code: " + symbolCode);
                System.out.println("Scale: " + scale);
            }
        }
        catch(Exception excf)
        {
            System.err.println(excf.getMessage());
            excf.printStackTrace();
        }

        //ErrorLogger.LogMessage("MultiPointHandler", "RenderSymbol()", "exit RenderSymbol", Level.FINER);
        return null;

    }
    
    private static SymbolInfo MilStdSymbolToSymbolInfo(MilStdSymbol symbol)
    {
        SymbolInfo si = null;
        
        ArrayList<TextInfo> tiList = new ArrayList<TextInfo>();
        ArrayList<LineInfo> liList = new ArrayList<LineInfo>();
        
        TextInfo tiTemp = null;
        LineInfo liTemp = null;
        ShapeInfo siTemp = null;
        
        ArrayList<ShapeInfo> lines = symbol.getSymbolShapes();
        ArrayList<ShapeInfo> modifiers = symbol.getModifierShapes();
        
        int lineCount = lines.size();
        int modifierCount = modifiers.size();
        for(int i =0; i < lineCount; i++)
        {
            siTemp = lines.get(i);
            if(siTemp.getPolylines()!= null)
            {
                liTemp = new LineInfo();
                liTemp.setFillColor(siTemp.getFillColor());
                liTemp.setLineColor(siTemp.getLineColor());
                liTemp.setPolylines(siTemp.getPolylines());
                liTemp.setStroke(siTemp.getStroke());
                liList.add(liTemp);
            }
        }
        
        for(int j =0; j < modifierCount; j++)
        {
            tiTemp = new TextInfo();
            siTemp = modifiers.get(j);
            if(siTemp.getModifierString() != null)
            {
                tiTemp.setModifierString(siTemp.getModifierString());
                tiTemp.setModifierStringPosition(siTemp.getModifierStringPosition());
                tiTemp.setModifierStringAngle(siTemp.getModifierStringAngle());
                tiList.add(tiTemp);
            }       
        }
        si = new SymbolInfo(tiList, liList);
        return si;
    }


    /**
     * Populates a symbol with the modifiers from a JSON string.   This
     * function will overwrite any previously populated
     * modifier data.
     * @param jsonString a JSON formatted string containing all the symbol
     * modifier data.
     * @param symbol  An existing MilStdSymbol
     * @return
     */
    private static boolean populateModifiers(String jsonString, MilStdSymbol symbol) {
        //ErrorLogger.LogMessage(String.valueOf(rect), Boolean.TRUE);
        // Stores label modifiers for MilStdSymbol
        Map<String, String> modifierMap = new HashMap<String, String>();

        // Stores array graphic modifiers for MilStdSymbol;
        ArrayList<Double> altitudes = null;
        ArrayList<Double> azimuths = null;
        ArrayList<Double> distances = null;

        // Stores colors for symbol.
        String fillColor = null;
        String lineColor = null;
        
        int lineWidth = 0;
        
        String symbolFillIDs = null;
        String symbolFillIconSize = null;

        try {

            JSONObject jsonModifiersString = new JSONObject(jsonString);
            JSONObject jsonModifiersArray =
            jsonModifiersString.getJSONObject("modifiers");


            // The following attirubtes are labels.  All of them
            // are strings and can be added on the creation of the
            // MilStdSymbol by adding to a Map and passing in the
            // modifiers parameter.
            if (jsonModifiersArray.has(QUANTITY) && !jsonModifiersArray.isNull(QUANTITY)) {
                modifierMap.put(ModifiersTG.C_QUANTITY, jsonModifiersArray.getString(QUANTITY));
            }
            else if (jsonModifiersArray.has(ModifiersTG.C_QUANTITY) && !jsonModifiersArray.isNull(ModifiersTG.C_QUANTITY)) {
                modifierMap.put(ModifiersTG.C_QUANTITY, jsonModifiersArray.getString(ModifiersTG.C_QUANTITY));
            }
            //                if (jsonModifiersArray.has(REINFORCE_OR_REDUCED)) {
            //                    modifierMap.put(ModifiersTG.REINFORCE_OR_REDUCED, jsonModifiersArray.getString(REINFORCE_OR_REDUCED));
            //                }
            //                if (jsonModifiersArray.has(STAFF_COMMENTS)) {
            //                    modifierMap.put(STAFF_COMMENTS, jsonModifiersArray.getString(STAFF_COMMENTS));
            //                }
            if (jsonModifiersArray.has(ADDITIONAL_INFO_1) && !jsonModifiersArray.isNull(ADDITIONAL_INFO_1)) {
                modifierMap.put(ModifiersTG.H_ADDITIONAL_INFO_1, jsonModifiersArray.getString(ADDITIONAL_INFO_1));
            }
            else if (jsonModifiersArray.has(ModifiersTG.H_ADDITIONAL_INFO_1) && !jsonModifiersArray.isNull(ModifiersTG.H_ADDITIONAL_INFO_1)) {
                modifierMap.put(ModifiersTG.H_ADDITIONAL_INFO_1, jsonModifiersArray.getString(ModifiersTG.H_ADDITIONAL_INFO_1));
            }
            
            if (jsonModifiersArray.has(ADDITIONAL_INFO_2) && !jsonModifiersArray.isNull(ADDITIONAL_INFO_2)) {
                modifierMap.put(ModifiersTG.H1_ADDITIONAL_INFO_2, jsonModifiersArray.getString(ADDITIONAL_INFO_2));
            }
            else if (jsonModifiersArray.has(ModifiersTG.H1_ADDITIONAL_INFO_2) && !jsonModifiersArray.isNull(ModifiersTG.H1_ADDITIONAL_INFO_2)) {
                modifierMap.put(ModifiersTG.H1_ADDITIONAL_INFO_2, jsonModifiersArray.getString(ModifiersTG.H1_ADDITIONAL_INFO_2));
            }
            
            if (jsonModifiersArray.has(ADDITIONAL_INFO_3) && !jsonModifiersArray.isNull(ADDITIONAL_INFO_3)) {
                modifierMap.put(ModifiersTG.H2_ADDITIONAL_INFO_3, jsonModifiersArray.getString(ADDITIONAL_INFO_3));
            }
            else if (jsonModifiersArray.has(ModifiersTG.H2_ADDITIONAL_INFO_3) && !jsonModifiersArray.isNull(ModifiersTG.H2_ADDITIONAL_INFO_3)) {
                modifierMap.put(ModifiersTG.H2_ADDITIONAL_INFO_3, jsonModifiersArray.getString(ModifiersTG.H2_ADDITIONAL_INFO_3));
            }
            //                if (jsonModifiersArray.has(EVALUATION_RATION)) {
            //                    modifierMap.put(ModifiersTG. EVALUATION_RATION, jsonModifiersArray.getString(EVALUATION_RATION));
            //                }
            //                if (jsonModifiersArray.has(COMBAT_EFFECTIVENESS)) {
            //                    modifierMap.put(COMBAT_EFFECTIVENESS, jsonModifiersArray.getString(COMBAT_EFFECTIVENESS));
            //                }
            //                if (jsonModifiersArray.has(SIGNATURE_EQUIPMENT)) {
            //                    modifierMap.put(SIGNATURE_EQUIPMENT, jsonModifiersArray.getString(SIGNATURE_EQUIPMENT));
            //                }
            //                if (jsonModifiersArray.has(HIGHER_FORMATION)) {
            //                    modifierMap.put(HIGHER_FORMATION, jsonModifiersArray.getString(HIGHER_FORMATION));
            //                }
            if (jsonModifiersArray.has(HOSTILE) && !jsonModifiersArray.isNull(HOSTILE)) {
                modifierMap.put(ModifiersTG.N_HOSTILE, jsonModifiersArray.getString(HOSTILE));
            }
            else if (jsonModifiersArray.has(ModifiersTG.N_HOSTILE) && !jsonModifiersArray.isNull(ModifiersTG.N_HOSTILE)) {
                modifierMap.put(ModifiersTG.N_HOSTILE, jsonModifiersArray.getString(ModifiersTG.N_HOSTILE));
            }
            //                if (jsonModifiersArray.has(IFFSIFF)) {
            //                    modifierMap.put(IFFSIFF, jsonModifiersArray.getString(IFFSIFF));
            //                }
            if (jsonModifiersArray.has(DIRECTION_OF_MOVEMENT) && !jsonModifiersArray.isNull(DIRECTION_OF_MOVEMENT)) {
                double directionOfMovement = jsonModifiersArray.getDouble(DIRECTION_OF_MOVEMENT);
                modifierMap.put(ModifiersTG.Q_DIRECTION_OF_MOVEMENT, Double.toString(directionOfMovement));
            }
            else if (jsonModifiersArray.has(ModifiersTG.Q_DIRECTION_OF_MOVEMENT) && !jsonModifiersArray.isNull(ModifiersTG.Q_DIRECTION_OF_MOVEMENT)) {
                double directionOfMovement = jsonModifiersArray.getDouble(ModifiersTG.Q_DIRECTION_OF_MOVEMENT);
                modifierMap.put(ModifiersTG.Q_DIRECTION_OF_MOVEMENT, Double.toString(directionOfMovement));
            }
            
            if (jsonModifiersArray.has(UNIQUE_DESIGNATION_1) && !jsonModifiersArray.isNull(UNIQUE_DESIGNATION_1)) {
                modifierMap.put(ModifiersTG.T_UNIQUE_DESIGNATION_1, jsonModifiersArray.getString(UNIQUE_DESIGNATION_1));
            }
            else if (jsonModifiersArray.has(ModifiersTG.T_UNIQUE_DESIGNATION_1) && !jsonModifiersArray.isNull(ModifiersTG.T_UNIQUE_DESIGNATION_1)) {
                modifierMap.put(ModifiersTG.T_UNIQUE_DESIGNATION_1, jsonModifiersArray.getString(ModifiersTG.T_UNIQUE_DESIGNATION_1));
            }
            
            if (jsonModifiersArray.has(UNIQUE_DESIGNATION_2) && !jsonModifiersArray.isNull(UNIQUE_DESIGNATION_2)) {
                modifierMap.put(ModifiersTG.T1_UNIQUE_DESIGNATION_2, jsonModifiersArray.getString(UNIQUE_DESIGNATION_2));
            }
            else if (jsonModifiersArray.has(ModifiersTG.T1_UNIQUE_DESIGNATION_2) && !jsonModifiersArray.isNull(ModifiersTG.T1_UNIQUE_DESIGNATION_2)) {
                modifierMap.put(ModifiersTG.T1_UNIQUE_DESIGNATION_2, jsonModifiersArray.getString(ModifiersTG.T1_UNIQUE_DESIGNATION_2));
            }
            
            if (jsonModifiersArray.has(EQUIPMENT_TYPE) && !jsonModifiersArray.isNull(EQUIPMENT_TYPE)) {
                modifierMap.put(ModifiersTG.V_EQUIP_TYPE, jsonModifiersArray.getString(EQUIPMENT_TYPE));
            }
            else if (jsonModifiersArray.has(ModifiersTG.V_EQUIP_TYPE) && !jsonModifiersArray.isNull(ModifiersTG.V_EQUIP_TYPE)) {
                modifierMap.put(ModifiersTG.V_EQUIP_TYPE, jsonModifiersArray.getString(ModifiersTG.V_EQUIP_TYPE));
            }
            
            if (jsonModifiersArray.has(DATE_TIME_GROUP_1) && !jsonModifiersArray.isNull(DATE_TIME_GROUP_1)) {
                modifierMap.put(ModifiersTG.W_DTG_1, jsonModifiersArray.getString(DATE_TIME_GROUP_1));
            }
            else if (jsonModifiersArray.has(ModifiersTG.W_DTG_1) && !jsonModifiersArray.isNull(ModifiersTG.W_DTG_1)) {
                modifierMap.put(ModifiersTG.W_DTG_1, jsonModifiersArray.getString(ModifiersTG.W_DTG_1));
            }
            
            if (jsonModifiersArray.has(DATE_TIME_GROUP_2) && !jsonModifiersArray.isNull(DATE_TIME_GROUP_2)) {
                modifierMap.put(ModifiersTG.W1_DTG_2, jsonModifiersArray.getString(DATE_TIME_GROUP_2));
            }
            else if (jsonModifiersArray.has(ModifiersTG.W1_DTG_2) && !jsonModifiersArray.isNull(ModifiersTG.W1_DTG_2)) {
                modifierMap.put(ModifiersTG.W1_DTG_2, jsonModifiersArray.getString(ModifiersTG.W1_DTG_2));
            }
            //                if (jsonModifiersArray.has(LOCATION)) {
            //                    modifierMap.put(LOCATION, jsonModifiersArray.getString(LOCATION));
            //                }
            //                if (jsonModifiersArray.has(SPEED)) {
            //                    modifierMap.put(SPEED, jsonModifiersArray.getString(SPEED));
            //                }
            //                if (jsonModifiersArray.has(SPECIAL_C2_HQ)) {
            //                    modifierMap.put(SPECIAL_C2_HQ, jsonModifiersArray.getString(SPECIAL_C2_HQ));
            //                }

            // These guys store array values.  Put in appropriate data strucutre
            // for MilStdSymbol.
            if (jsonModifiersArray.has(ALTITUDE_DEPTH) && !jsonModifiersArray.isNull(ALTITUDE_DEPTH)) {
                JSONArray jsonAltitudeArray = jsonModifiersArray.getJSONArray(ALTITUDE_DEPTH);
                altitudes = new ArrayList<Double>();
                for (int i = 0; i < jsonAltitudeArray.length(); i++) {
                    altitudes.add(jsonAltitudeArray.getDouble(i));
                }
            }
            else if (jsonModifiersArray.has(ModifiersTG.X_ALTITUDE_DEPTH) && !jsonModifiersArray.isNull(ModifiersTG.X_ALTITUDE_DEPTH)) {
                JSONArray jsonAltitudeArray = jsonModifiersArray.getJSONArray(ModifiersTG.X_ALTITUDE_DEPTH);
                altitudes = new ArrayList<Double>();
                for (int i = 0; i < jsonAltitudeArray.length(); i++) {
                    altitudes.add(jsonAltitudeArray.getDouble(i));
                }
            }
            
            if (jsonModifiersArray.has(DISTANCE) && !jsonModifiersArray.isNull(DISTANCE)) {
                JSONArray jsonDistanceArray = jsonModifiersArray.getJSONArray(DISTANCE);
                distances = new ArrayList<Double>();
                for (int i = 0; i < jsonDistanceArray.length(); i++) {
                    distances.add(jsonDistanceArray.getDouble(i));
                }
            }
            else if (jsonModifiersArray.has(ModifiersTG.AM_DISTANCE) && !jsonModifiersArray.isNull(ModifiersTG.AM_DISTANCE)) {
                JSONArray jsonDistanceArray = jsonModifiersArray.getJSONArray(ModifiersTG.AM_DISTANCE);
                distances = new ArrayList<Double>();
                for (int i = 0; i < jsonDistanceArray.length(); i++) {
                    distances.add(jsonDistanceArray.getDouble(i));
                }
            }
            
            if (jsonModifiersArray.has(AZIMUTH) && !jsonModifiersArray.isNull(AZIMUTH)) {
                JSONArray jsonAzimuthArray = jsonModifiersArray.getJSONArray(AZIMUTH);
                azimuths = new ArrayList<Double>();
                for (int i = 0; i < jsonAzimuthArray.length(); i++) {
                    azimuths.add(jsonAzimuthArray.getDouble(i));
                }
            }
            else if (jsonModifiersArray.has(ModifiersTG.AN_AZIMUTH) && !jsonModifiersArray.isNull(ModifiersTG.AN_AZIMUTH)) {
                JSONArray jsonAzimuthArray = jsonModifiersArray.getJSONArray(ModifiersTG.AN_AZIMUTH);
                azimuths = new ArrayList<Double>();
                for (int i = 0; i < jsonAzimuthArray.length(); i++) {
                    azimuths.add(jsonAzimuthArray.getDouble(i));
                }
            }

            // These properties are ints, not labels, they are colors.//////////////////
            if (jsonModifiersArray.has(FILL_COLOR) && !jsonModifiersArray.isNull(FILL_COLOR)) {
                fillColor = jsonModifiersArray.getString(FILL_COLOR);
            }
            if (jsonModifiersArray.has(LINE_COLOR) && !jsonModifiersArray.isNull(LINE_COLOR)) {
                lineColor = jsonModifiersArray.getString(LINE_COLOR);
            }
            
            if (jsonModifiersArray.has(LINE_THICKNESS) && !jsonModifiersArray.isNull(LINE_THICKNESS)) {
                lineWidth = jsonModifiersArray.getInt(LINE_THICKNESS);
            }
            
            // These are for when we create a area fill that is comprised of symbols//////////
            if (jsonModifiersArray.has(SYMBOL_FILL_IDS) && !jsonModifiersArray.isNull(SYMBOL_FILL_IDS)) {
                modifierMap.put(SYMBOL_FILL_IDS, jsonModifiersArray.getString(SYMBOL_FILL_IDS));
            }
            if (jsonModifiersArray.has(SYMBOL_LINE_IDS) && !jsonModifiersArray.isNull(SYMBOL_LINE_IDS)) {
                modifierMap.put(SYMBOL_LINE_IDS, jsonModifiersArray.getString(SYMBOL_LINE_IDS));
            }
            if (jsonModifiersArray.has(SYMBOL_FILL_ICON_SIZE) && !jsonModifiersArray.isNull(SYMBOL_FILL_ICON_SIZE)) {
                modifierMap.put(SYMBOL_FILL_ICON_SIZE, jsonModifiersArray.getString(SYMBOL_FILL_ICON_SIZE));
            }

          
        } catch (JSONException je) {
            System.out.println("Failed to parse modifier string in "
                    + "MultiPointHandler.RenderSymbol. Continuing processing "
                    + "the drawing of the graphic");
            System.out.println("Json String: " + String.valueOf(jsonString));
            System.out.println(je.getMessage());
            System.out.println(JavaRendererUtilities.getStackTrace(je));
            return false;
        }

        try
        {
            symbol.setModifierMap(modifierMap);

            if (fillColor != null) {
                symbol.setFillColor(SymbolUtilities.getColorFromHexString(fillColor));
            }
            else
            {
                symbol.setFillColor(null);
            }

            if (lineColor != null) {
                symbol.setLineColor(SymbolUtilities.getColorFromHexString(lineColor));
            }
            
            if (lineWidth > 0) {
                symbol.setLineWidth(lineWidth);
            }

            // Check grpahic modifiers variables.  If we set earlier, populate
            // the fields, otherwise, ignore.
            if (altitudes != null) {
                symbol.setModifiers_AM_AN_X(ModifiersTG.X_ALTITUDE_DEPTH, altitudes);
            }
            if (distances != null) {
                symbol.setModifiers_AM_AN_X(ModifiersTG.AM_DISTANCE, distances);
            }

            if (azimuths != null) {
                symbol.setModifiers_AM_AN_X(ModifiersTG.AN_AZIMUTH, azimuths);
            }

            //Check if sector range fan has required min range
            if(SymbolUtilities.getBasicSymbolID(symbol.getSymbolID()).equals("G*F*AXS---****X"))
            {
                if(symbol.getModifiers_AM_AN_X(ModifiersTG.AN_AZIMUTH)!=null &&
                    symbol.getModifiers_AM_AN_X(ModifiersTG.AM_DISTANCE)!=null)
                {
                    int anCount = symbol.getModifiers_AM_AN_X(ModifiersTG.AN_AZIMUTH).size();
                    int amCount = symbol.getModifiers_AM_AN_X(ModifiersTG.AM_DISTANCE).size();
                    ArrayList<Double> am = null;
                    if(amCount < ((anCount/2) + 1))
                    {
                        am = symbol.getModifiers_AM_AN_X(ModifiersTG.AM_DISTANCE);
                        if(am.get(0)!=0.0)
                        {
                            am.add(0, 0.0);
                        }
                    }
                }
            }
        }
        catch(Exception exc)
        {
            System.out.println(exc.getMessage());
            exc.printStackTrace();
        }
        return true;

    }

    /**
     * FOR DEUTCH USE ONLY
     * @param symbolCode
     * @param controlPoints
     * @param scale
     * @param bbox
     * @param shapes
    @deprecated to make sure no one else is using it.
     */
    public static IPointConversion RenderSymbol2(String symbolCode,
            String controlPoints,
            Double scale,
            String bbox,
            ArrayList<ShapeInfo> shapes,
            ArrayList<ShapeInfo> modifiers)//,
            //ArrayList<ShapeInfo>shapes)
    {

        boolean normalize = false;
        StringBuilder jsonOutput = new StringBuilder();
        String jsonContent = "";
        Rectangle rect = null;
        int j=0;
        String[] coordinates = controlPoints.split(" ");
        TGLight tgl = new TGLight();
        ArrayList<Point2D.Double> geoCoords = new ArrayList<Point2D.Double>();
        int len = coordinates.length;

        IPointConversion ipc = null;

        //Deutch moved section 6-29-11
        Double left = 0.0;
        Double right = 0.0;
        Double top = 0.0;
        Double bottom = 0.0;
        Point2D temp = null;
        int width=0;
        int height=0;
        int leftX = 0;
        int topY = 0;
        int bottomY = 0;
        int rightX = 0;
        ArrayList<Point2D.Double> bboxCoords = null;
        Point2D ptGeoUL;
        if (bbox != null && bbox.equals("") == false) 
        {
            String[] bounds = null;
            if(bbox.contains(" "))//trapezoid or polygon
            {
                bboxCoords = new ArrayList<Point2D.Double>();
                double x = 0;
                double y = 0;
                String[] coords = bbox.split(" ");
                String[] arrCoord;
                for(String coord : coords)
                {
                    arrCoord = coord.split(",");
                    x = Double.valueOf(arrCoord[0]);
                    y = Double.valueOf(arrCoord[1]);
                    bboxCoords.add(new Point2D.Double(x, y));
                }
                //use the upper left corner of the MBR containing geoCoords
                //so lowest possible pxiels values for the trapezoid points are 0,0
                ptGeoUL=getGeoUL(bboxCoords);
                left=ptGeoUL.getX();
                top=ptGeoUL.getY();
                //bboxCoords need to be in pixels
                ipc = new PointConverter(left, top, scale);
                //diagnostic
                //the renderer is going to expand the trapezoid by 20 pixels
                //so that it can cut off the connector lines on the boundaries.
                //Shift the converter by 20x20 pixels here to shift the trapezoid   
                //so that it will effectively have the same origin after it is expanded
                Point2D ptPixels=null;
                ptPixels=new Point2D.Double(20,20);                
                Point2D ptGeo=ipc.PixelsToGeo(ptPixels);
                IPointConversion ipcTemp=new PointConverter(ptGeo.getX(),ptGeo.getY(),scale);
                for(j=0;j<bboxCoords.size();j++)
                {
                    ptGeo=bboxCoords.get(j);
                    ptPixels=ipcTemp.GeoToPixels(ptGeo);                    
                    bboxCoords.set(j,(Point2D.Double)ptPixels);
                }                
            }
            else//rectangle
            {
                bounds = bbox.split(",");
                left = Double.valueOf(bounds[0]).doubleValue();
                right = Double.valueOf(bounds[2]).doubleValue();
                top = Double.valueOf(bounds[3]).doubleValue();
                bottom = Double.valueOf(bounds[1]).doubleValue();
                ipc = new PointConverter(left, top, scale);
            }
                                                                    
            //added 2 lines Deutch 6-29-11
            //controlLong = left;
            //controlLat = top;
            //end section

            //new conversion
            //TODO: swap two lines below when ready for coordinate update
            //ipc = new PointConverter(left, top, scale);
            //ipc = new PointConverter(left, top, right, bottom, scale);

            if(bboxCoords==null)
            {
                temp = ipc.GeoToPixels(new Point2D.Double(left, top));
                leftX = (int)temp.getX();
                topY = (int)temp.getY();

                temp = ipc.GeoToPixels(new Point2D.Double(right, bottom));
                bottomY = (int)temp.getY();
                rightX = (int)temp.getX();
                //////////////////

                width = (int) Math.abs(rightX - leftX);
                height = (int) Math.abs(bottomY - topY);

                //Rectangle rect = new Rectangle(leftX,topY,width,height);
                rect = new Rectangle(leftX, topY, width, height);
            }
            //System.out.println("Clip Bounds: ");
            //System.out.println(rect.toString());
        } 
        else 
        {
            rect = null;
        }
        //end section

        //System.out.println("Pixel Coords: ");
        for (int i = 0; i < len; i++) {
            String[] coordPair = coordinates[i].split(",");
            Double latitude = Double.valueOf(coordPair[1].trim()).doubleValue();
            Double longitude = Double.valueOf(coordPair[0].trim()).doubleValue();
            geoCoords.add(new Point2D.Double(longitude, latitude));
            //pixels.add(new Point2D.Double(longitudeX, latitudeY));

            //System.out.println(String.valueOf(latitudeY) + ", " + String.valueOf(longitudeX));

            //ipc = new PointConverter(controlLong, controlLat, scale);
        }
        if(ipc==null)
        {
            Point2D ptCoordsUL=getGeoUL(geoCoords);            
            ipc = new PointConverter(ptCoordsUL.getX(), ptCoordsUL.getY(), scale);            
        }
        //M. Deutch 9-26-11
        //rect=null;
        if(crossesIDL(geoCoords)==true)
            normalize=true;
        else
            normalize=false;

        //seems to work ok at world view
        if(normalize)
        {
            NormalizeGECoordsToGEExtents(0,360,geoCoords);
        }

        tgl.set_SymbolId(symbolCode);// "GFGPSLA---****X" AMBUSH symbol code
        tgl.set_Pixels(null);

        try
        {
            Map<String,String> modifierMap = new HashMap<String, String>();
            MilStdSymbol mSymbol = new MilStdSymbol(symbolCode, null, geoCoords, modifierMap);
            tgl = clsRenderer.createTGLightFromMilStdSymbol(mSymbol, ipc);

            if(bboxCoords == null)
                clsRenderer.render_GE(tgl, shapes, modifiers, ipc, rect);
            else
                clsRenderer.render_GE(tgl, shapes, modifiers, ipc, bboxCoords);
            
            jsonOutput.append("{\"type\":\"symbol\",");
            jsonContent = JSONize(shapes, modifiers, ipc, true, normalize);
            jsonOutput.append(jsonContent);
            jsonOutput.append("}");

        }
        catch(Exception exc)
        {
            jsonOutput = new StringBuilder();
            jsonOutput.append("{\"type\":\"error\",\"error\":\"There was an error creating the MilStdSymbol - ");
            jsonOutput.append(exc.getMessage() + " - ");
            //jsonOutput.append(ErrorLogger.getStackTrace(exc));
            jsonOutput.append("\"}");
        }


        boolean debug = true;
        if(debug == true)
        {
            System.out.println("Symbol Code: " + symbolCode);
            System.out.println("Scale: " + scale);
            System.out.println("BBOX: " + bbox);
            if(controlPoints != null)
                System.out.println("Geo Points: " + controlPoints);
            //if(pixels != null)
                //System.out.println("Pixel: " + pixels.toString());
            if(bbox != null)
                System.out.println("geo bounds: " + bbox);
            if(rect != null)
                System.out.println("pixel bounds: " + rect.toString());
            if(jsonOutput != null)
                System.out.println(jsonOutput.toString());
        }
        //return jsonOutput.toString();
        return ipc;

    }

    private static String KMLize(String id, String name, 
            String description, 
            String symbolCode,
            ArrayList<ShapeInfo> shapes, 
            ArrayList<ShapeInfo> modifiers, 
            IPointConversion ipc, Boolean geMap, 
            boolean normalize) {
        
        StringBuilder kml = new StringBuilder();

        ShapeInfo tempModifier = null;        

        int len = shapes.size();
        kml.append("<Folder id=\"" + id + "\">");
        kml.append("<name>" + name + "</name>");        
        kml.append("<visibility>1</visibility>");
        for (int i = 0; i < len; i++) {

            String shapesToAdd = ShapeToKMLString(id, name, description, symbolCode, shapes.get(i), ipc, geMap, normalize);
            kml.append(shapesToAdd);
        }

        int len2 = modifiers.size();

        for (int j = 0; j < len2; j++) {

            tempModifier = modifiers.get(j);

            if(geMap)//if using google earth
                AdjustModifierPointToCenter(tempModifier);

            String labelsToAdd = LabelToKMLString(id, j, tempModifier, ipc, normalize);
            kml.append(labelsToAdd);
        }

        kml.append("</Folder>");
        return kml.toString();
    }

    private static String JSONize(ArrayList<ShapeInfo> shapes, ArrayList<ShapeInfo> modifiers, IPointConversion ipc, Boolean geMap, boolean normalize) {
        String polygons = "";
        String lines = "";
        String labels = "";
        String jstr = "";
        ShapeInfo tempModifier = null;

        int len = shapes.size();
        for (int i = 0; i < len; i++) {
            if (jstr.length() > 0) {
                jstr += ",";
            }
            String shapesToAdd = ShapeToJSONString(shapes.get(i), ipc, geMap, normalize);
            if (shapesToAdd.length() > 0) {
                if (shapesToAdd.startsWith("line", 2)) {
                    if (lines.length() > 0) {
                        lines += ",";
                    }

                    lines += shapesToAdd;
                } else if (shapesToAdd.startsWith("polygon", 2)) {
                    if (polygons.length() > 0) {
                        polygons += ",";
                    }

                    polygons += shapesToAdd;
                }
            }
        }

        jstr += "\"polygons\": [" + polygons + "],"
                + "\"lines\": [" + lines + "],";
        int len2 = modifiers.size();
        labels = "";
        for (int j = 0; j < len2; j++) {
            tempModifier = modifiers.get(j);
            if(geMap)
                AdjustModifierPointToCenter(tempModifier);
            String labelsToAdd = LabelToJSONString(tempModifier, ipc, normalize);
            if (labelsToAdd.length() > 0) {
                if (labels.length() > 0) {
                    labels += ",";
                }

                labels += labelsToAdd;

            }
        }
        jstr += "\"labels\": [" + labels + "]";
        return jstr;
    }
    

    /**
     * 
     * @param urlImage
     * @param ipc
     * @param symbolBounds
     * @param normalize
     * @return 
     */
    private static String GenerateGroundOverlayKML(
            String urlImage, IPointConversion ipc,
            Rectangle symbolBounds,
            boolean normalize)//, ArrayList<ShapeInfo> shapes)
    {
        //int shapeType = -1;
        double x = 0;
        double y = 0;
        double height = 0;
        double width = 0;
        //ShapeInfo siTemp = null;
        //int shapeCount = shapes.size();
        StringBuilder sb = new StringBuilder();
        Boolean lineFill = false;
        Map<String,String> params = null;
        int symbolSize = 0;
        int imageOffset = 0;

        
        try
        {
            //if it's a line pattern, we need to know how big the symbols
            //are so we can increase the size of the image.
            int index = -1;
            index = urlImage.indexOf(SYMBOL_LINE_IDS);
            
            if(index > 0)//if(urlImage contains SYMBOL_LINE_IDS)
            {
                lineFill=true;
                params = SinglePointRendererService.getInstance().processParams(urlImage);
                if(params.containsKey(SYMBOL_FILL_ICON_SIZE))
                {
                    String size = (String)params.get(SYMBOL_FILL_ICON_SIZE);
                    symbolSize = Integer.decode(size);// getInteger(size);
                }
                else
                {
                    symbolSize = AreaSymbolFill.DEFAULT_SYMBOL_SIZE;
                }
                imageOffset = (symbolSize/2) +3;//+3 to make room for rotation
            }
            
            //get the bounds of the image
            Rectangle2D bounds = null;
//            for(int i = 0; i < shapeCount; i++)//(ShapeInfo si : shapes)
//            {
//                siTemp = shapes.get(i);
//                shapeType = siTemp.getShapeType(); 
//                height = 0;
//                width = 0;
//                
//                if(shapeType==ShapeInfo.SHAPE_TYPE_POLYLINE)
//                {
//                    if(bounds==null)
//                        bounds = siTemp.getBounds();
//                    else
//                        bounds.union(bounds, siTemp.getBounds(), bounds);
//                }
//                //System.out.println(bounds.toString());
//            }
            bounds = symbolBounds;
//            System.out.println(urlImage);
//            System.out.println(SYMBOL_LINE_IDS);
//            System.out.println("index: " + String.valueOf(index));
//            System.out.println("kml offset: " + String.valueOf(imageOffset) + " " + lineFill.toString());
            height = bounds.getHeight()+(imageOffset*2);
            width = bounds.getWidth()+(imageOffset*2);
            x = bounds.getX()-imageOffset;
            y = bounds.getY()-imageOffset;
            

            Point2D coord = (Point2D) new Point2D.Double(x, y);
            Point2D topLeft = ipc.PixelsToGeo(coord);
            coord = (Point2D) new Point2D.Double(x+width,y+height);
            Point2D bottomRight = ipc.PixelsToGeo(coord);
            
            //get middle values///TEST//////////////////////////////////////////
            //less accurate than the above four lines.
            /*
            double north = 0;
            double south = 0;
            double east = 0;
            double west = 0;
            
            coord = (Point2D) new Point2D.Double(bounds.getX(),bounds.getCenterY());
            topLeft = ipc.PixelsToGeo(coord);
            west = topLeft.getX();
            coord = (Point2D) new Point2D.Double(bounds.getCenterX(),bounds.getY());
            topLeft = ipc.PixelsToGeo(coord);
            north = topLeft.getY();
            
            coord = (Point2D) new Point2D.Double(bounds.getX()+bounds.getWidth(),bounds.getCenterY());
            bottomRight = ipc.PixelsToGeo(coord);
            east = bottomRight.getX();
            coord = (Point2D) new Point2D.Double(bounds.getCenterX(),bounds.getY()+bounds.getHeight());
            bottomRight = ipc.PixelsToGeo(coord);
            south = bottomRight.getY();
            
            topLeft = new Point2D.Double(west, north);
            bottomRight = new Point2D.Double(east, south);//*/
            ////////////////////////////////////////////////////////////////////

            if(normalize)
            {
                topLeft=NormalizeCoordToGECoord(topLeft);
                bottomRight=NormalizeCoordToGECoord(bottomRight);
            }

            String cdataStart = "<![CDATA[";
            String cdataEnd = "]]>";
            //build kml
            sb.append("<GroundOverlay>");
                sb.append("<name>symbol fill</name>");
                //sb.append("<visibility>0</visibility>");
                sb.append("<description>symbol fill</description>");
                sb.append("<Icon>");
                    sb.append("<href>");
                    sb.append(cdataStart);
                    sb.append(urlImage);
                    sb.append(cdataEnd);
                    sb.append("</href>");
                sb.append("</Icon>");
                sb.append("<LatLonBox>");
                    sb.append("<north>");
                    sb.append(String.valueOf(topLeft.getY()));
                    sb.append("</north>");
                    sb.append("<south>");
                    sb.append(String.valueOf(bottomRight.getY()));
                    sb.append("</south>");
                    sb.append("<east>");
                    sb.append(String.valueOf(bottomRight.getX()));
                    sb.append("</east>");
                    sb.append("<west>");
                    sb.append(String.valueOf(topLeft.getX()));
                    sb.append("</west>");
                    sb.append("<rotation>");
                    sb.append(0);
                    sb.append("</rotation>");
                sb.append("</LatLonBox>");
            sb.append("</GroundOverlay>");
        }
        catch(Exception exc)
        {
            System.out.println(exc.getMessage());
            exc.printStackTrace();
        }
        String kml = sb.toString();
        return kml;
    }
    
    
     private static void MakeWWReady(
            ArrayList<ShapeInfo> shapes, 
            ArrayList<ShapeInfo> modifiers, 
            IPointConversion ipc, 
            boolean normalize) 
     {
        ShapeInfo temp = null;
        int len = shapes.size();
        for (int i = 0; i < len; i++) 
        {

            temp = ShapeToWWReady(shapes.get(i), ipc, normalize);
            shapes.set(i, temp);
            
        }

        int len2 = modifiers.size();
        ShapeInfo tempModifier = null;
        for (int j = 0; j < len2; j++) 
        {

            tempModifier = modifiers.get(j);

            //Do we need this for World Wind?
            //AdjustModifierPointToCenter(tempModifier);
            tempModifier = LabelToWWReady(tempModifier, ipc, normalize);
            modifiers.set(j, tempModifier);
            
        }

        
        //return tempModifier;
    }
    
    private static String ShapeToKMLString(String id, 
            String name, 
            String description, 
            String symbolCode, 
            ShapeInfo shapeInfo, 
            IPointConversion ipc, 
            Boolean geMap,              
            boolean normalize) {
        
        StringBuilder kml = new StringBuilder();

        Color lineColor = null;
        Color fillColor = null;
        String googleLineColor = null;
        String googleFillColor = null;
      
        String lineStyleId = "lineColor";              
       
        BasicStroke stroke = null;
        int lineWidth = 4;
        
        symbolCode = JavaRendererUtilities.normalizeSymbolCode(symbolCode);

        kml.append("<Placemark id=\"" + id + "_mg" + "\">");
        kml.append("<description>" + "<b>" + name + "</b><br/>" + "\n" + description + "</description>");
        kml.append("<Style id=\"" + lineStyleId + "\">");

        lineColor = shapeInfo.getLineColor();
        if (lineColor != null) {
            googleLineColor = Integer.toHexString(shapeInfo.getLineColor().getRGB());
            
            stroke = (BasicStroke)shapeInfo.getStroke();
            
            if(stroke != null)
            {
                lineWidth = (int)stroke.getLineWidth();
                lineWidth++;
                //System.out.println("lineWidth: " + String.valueOf(lineWidth));
            }
                            
            while(googleLineColor.length() < 8)
            {                
                googleLineColor = "0" + googleLineColor;
            }
            
            if (geMap) {
                googleLineColor = JavaRendererUtilities.ARGBtoABGR(googleLineColor);
            } 
            
            kml.append("<LineStyle>");
            kml.append("<color>" + googleLineColor + "</color>");
            kml.append("<colorMode>normal</colorMode>");
            kml.append("<width>" + String.valueOf(lineWidth) + "</width>");
            kml.append("</LineStyle>");
        }

        fillColor = shapeInfo.getFillColor();                
        if (fillColor != null) {
            googleFillColor = Integer.toHexString(shapeInfo.getFillColor().getRGB());
            while(googleFillColor.length() < 8)
            {
                googleFillColor = "0" + googleFillColor;
            }

            if (geMap) {
                googleFillColor = JavaRendererUtilities.ARGBtoABGR(googleFillColor);
            } 

            kml.append("<PolyStyle>");
            kml.append("<color>" + googleFillColor + "</color>");
            kml.append("<colorMode>normal</colorMode>");
            kml.append("<fill>1</fill>");
            kml.append("<outline>0</outline>");
            kml.append("</PolyStyle>");
        }

        kml.append("</Style>");

        ArrayList shapesArray = shapeInfo.getPolylines();        
        int len = shapesArray.size();
        kml.append("<MultiGeometry>");

        for (int i = 0; i < len; i++) {
            ArrayList shape = (ArrayList) shapesArray.get(i);
            
            if (lineColor != null)
            {
                kml.append("<LineString>");
                kml.append("<tessellate>1</tessellate>");
                kml.append("<altitudeMode>clampToGround</altitudeMode>");
                kml.append("<coordinates>");

                for (int j = 0; j < shape.size(); j++) {
                    Point2D coord = (Point2D) shape.get(j);
                    Point2D geoCoord = ipc.PixelsToGeo(coord);
                    //M. Deutch 9-26-11
                    if(normalize)
                        geoCoord=NormalizeCoordToGECoord(geoCoord);

                    double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
                    double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;

                    kml.append(longitude);
                    kml.append(",");
                    kml.append(latitude);
                    kml.append(" ");
                }

                kml.append("</coordinates>");
                kml.append("</LineString>");
            }

            if (fillColor != null) {

                if(i==0)
                    kml.append("<Polygon>");
                //kml.append("<outerBoundaryIs>");
                if(i==1 && len>1)
                    kml.append("<innerBoundaryIs>");
                else
                    kml.append("<outerBoundaryIs>");
                kml.append("<LinearRing>");
                kml.append("<altitudeMode>clampToGround</altitudeMode>");
                kml.append("<tessellate>1</tessellate>");
                kml.append("<coordinates>");

                for (int j = 0; j < shape.size(); j++) {
                    Point2D coord = (Point2D) shape.get(j);
                    Point2D geoCoord = ipc.PixelsToGeo(coord);
                    //M. Deutch 9-26-11
                    //commenting these two lines seems to help with fill not go around the pole
                    //if(normalize)
                        //geoCoord=NormalizeCoordToGECoord(geoCoord);

                    double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
                    double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;

                    kml.append(longitude);
                    kml.append(",");
                    kml.append(latitude);
                    kml.append(" ");
                }

                kml.append("</coordinates>");                
                kml.append("</LinearRing>");                
                //kml.append("</outerBoundaryIs>");
                if(i==1 && len>1)
                    kml.append("</innerBoundaryIs>");
                else                
                    kml.append("</outerBoundaryIs>");
                if(i==len-1)
                    kml.append("</Polygon>");
            }
        }

        kml.append("</MultiGeometry>");
        kml.append("</Placemark>");

        return kml.toString();
    }
    
    
   private static ShapeInfo ShapeToWWReady(
        ShapeInfo shapeInfo, 
        IPointConversion ipc,        
        boolean normalize) 
   {

        ArrayList shapesArray = shapeInfo.getPolylines();        
        int len = shapesArray.size();
        

        for (int i = 0; i < len; i++) {
            ArrayList shape = (ArrayList) shapesArray.get(i);
            
            if (shapeInfo.getLineColor() != null)
            {
                

                for (int j = 0; j < shape.size(); j++) {
                    Point2D coord = (Point2D) shape.get(j);
                    Point2D geoCoord = ipc.PixelsToGeo(coord);
                    //M. Deutch 9-26-11
                    if(normalize)
                        geoCoord=NormalizeCoordToGECoord(geoCoord);

                    shape.set(j, geoCoord);
                    
                }

                
            }

            if (shapeInfo.getFillColor() != null) {

                for (int j = 0; j < shape.size(); j++) {
                    Point2D coord = (Point2D) shape.get(j);
                    Point2D geoCoord = ipc.PixelsToGeo(coord);
                    //M. Deutch 9-26-11
                    //commenting these two lines seems to help with fill not go around the pole
                    //if(normalize)
                        //geoCoord=NormalizeCoordToGECoord(geoCoord);

                    shape.set(j, geoCoord);
                }
            }
        }

        return shapeInfo;
    }
   
       private static ShapeInfo LabelToWWReady(ShapeInfo shapeInfo, 
               IPointConversion ipc, 
               boolean normalize) 
       {
        
           try
           {
                Point2D coord = (Point2D) new Point2D.Double(shapeInfo.getGlyphPosition().getX(), shapeInfo.getGlyphPosition().getY());
                Point2D geoCoord = ipc.PixelsToGeo(coord);
                //M. Deutch 9-26-11
                if(normalize)
                    geoCoord=NormalizeCoordToGECoord(geoCoord);
                double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
                double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;
                long angle = Math.round(shapeInfo.getModifierStringAngle());

                String text = shapeInfo.getModifierString();

                if (text != null && text.equals("") == false) 
                {
                    shapeInfo.setModifierStringPosition(geoCoord);
                } 
                else 
                {
                    return null;
                }
           }
           catch(Exception exc)
           {
                System.err.println(exc.getMessage());
                exc.printStackTrace();
           }

            return shapeInfo;
    }
//    
//    private static String generateKMLFillForDefaultGraphic(ArrayList shapesArray, String shapeFillColor, IPointConversion ipc, boolean normalize)
//    {
//        StringBuilder kml = new StringBuilder();        
//        
//        int len = shapesArray.size();
//        
//        for (int i = 0; i < len; i++) {
//            ArrayList shape = (ArrayList) shapesArray.get(i);
//
//            kml.append("<LineString>");
//            kml.append("<tessellate>1</tessellate>");
//            kml.append("<altitudeMode>clampToGround</altitudeMode>");
//            kml.append("<coordinates>");
//
//            for (int j = 0; j < shape.size(); j++) {
//                Point2D coord = (Point2D) shape.get(j);
//                Point2D geoCoord = ipc.PixelsToGeo(coord);
//                //M. Deutch 9-26-11
//                if(normalize)
//                    geoCoord=NormalizeCoordToGECoord(geoCoord);
//
//                double latitude = geoCoord.getY();
//                double longitude = geoCoord.getX();
//
//                kml.append(longitude);
//                kml.append(",");
//                kml.append(latitude);
//                kml.append(" ");
//            }
//
//            kml.append("</coordinates>");
//            kml.append("</LineString>");
//
//            if (shapeFillColor != null) {
//
//                kml.append("<Polygon>");
//                kml.append("<outerBoundaryIs>");
//                kml.append("<LinearRing>");
//                kml.append("<altitudeMode>clampToGround</altitudeMode>");
//                kml.append("<tessellate>1</tessellate>");
//                kml.append("<coordinates>");
//
//                for (int j = 0; j < shape.size(); j++) {
//                    Point2D coord = (Point2D) shape.get(j);
//                    Point2D geoCoord = ipc.PixelsToGeo(coord);
//                    //M. Deutch 9-26-11
//                    //commenting these two lines seems to help with fill not go around the pole
//                    //if(normalize)
//                        //geoCoord=NormalizeCoordToGECoord(geoCoord);
//
//                    double latitude = geoCoord.getY();
//                    double longitude = geoCoord.getX();
//
//                    kml.append(longitude);
//                    kml.append(",");
//                    kml.append(latitude);
//                    kml.append(" ");
//                }
//
//                kml.append("</coordinates>");
//                kml.append("</LinearRing>");
//                kml.append("</outerBoundaryIs>");
//                kml.append("</Polygon>");
//            }
//        }
//        
//        return kml.toString();
//    }
//    
//    private static String generateFillForCircularTarget(
//            ArrayList shapesArray, 
//            String shapeFillColor, 
//            IPointConversion ipc, 
//            boolean normalize)
//    {
//        StringBuilder kml = new StringBuilder();
//         // Save the coordinates of the first ring.
//        StringBuilder firstRingCoordinates = new StringBuilder();
//        
//        int len = shapesArray.size();
//        
//        for (int i = 0; i < len; i++) {
//            ArrayList shape = (ArrayList) shapesArray.get(i);
//
//            kml.append("<LineString>");
//            kml.append("<tessellate>1</tessellate>");
//            kml.append("<altitudeMode>clampToGround</altitudeMode>");
//            kml.append("<coordinates>");
//
//            for (int j = 0; j < shape.size(); j++) {
//                Point2D coord = (Point2D) shape.get(j);
//                Point2D geoCoord = ipc.PixelsToGeo(coord);
//                //M. Deutch 9-26-11
//                if(normalize)
//                    geoCoord=NormalizeCoordToGECoord(geoCoord);
//
//                double latitude = geoCoord.getY();
//                double longitude = geoCoord.getX();
//
//                kml.append(longitude);
//                kml.append(",");
//                kml.append(latitude);
//                kml.append(" ");
//
//                if (i == 0)
//                {
//                    firstRingCoordinates.append(longitude);
//                    firstRingCoordinates.append(",");
//                    firstRingCoordinates.append(latitude);
//                    firstRingCoordinates.append(" ");
//                }
//            }
//
//            kml.append("</coordinates>");
//            kml.append("</LineString>");
//
//
//            // Never fill the first ring.  Only subsequent values.
//            if (i > 0)
//            {
//                if (shapeFillColor != null) {
//
//                    kml.append("<Polygon>");
//                    kml.append("<outerBoundaryIs>");
//                    kml.append("<LinearRing>");
//                    kml.append("<altitudeMode>clampToGround</altitudeMode>");
//                    kml.append("<tessellate>1</tessellate>");
//                    kml.append("<coordinates>");
//
//                    for (int j = 0; j < shape.size(); j++) {
//                        Point2D coord = (Point2D) shape.get(j);
//                        Point2D geoCoord = ipc.PixelsToGeo(coord);
//                        //M. Deutch 9-26-11
//                        //commenting these two lines seems to help with fill not go around the pole
//                        //if(normalize)
//                            //geoCoord=NormalizeCoordToGECoord(geoCoord);
//
//                        double latitude = geoCoord.getY();
//                        double longitude = geoCoord.getX();
//
//                        kml.append(longitude);
//                        kml.append(",");
//                        kml.append(latitude);
//                        kml.append(" ");
//                    }
//
//                    kml.append("</coordinates>");
//                    kml.append("</LinearRing>");
//                    kml.append("</outerBoundaryIs>");
//
//                    kml.append("<innerBoundaryIs>");
//                    kml.append("<LinearRing>");
//                    kml.append("<altitudeMode>clampToGround</altitudeMode>");
//                    kml.append("<tessellate>1</tessellate>");
//                    kml.append("<coordinates>");
//
//                    kml.append(firstRingCoordinates);
//
//                    kml.append("</coordinates>");
//                    kml.append("</LinearRing>");
//                    kml.append("</innerBoundaryIs>");
//
//
//                    kml.append("</Polygon>");
//                }
//            }                
//        }
//        return kml.toString();
//    }

    /**
     * Google earth centers text on point rather than drawing from that point.
     * So we need to adjust the point to where the center of the text would be.
     * @param modifier
     */
    private static void AdjustModifierPointToCenter(ShapeInfo modifier)
    {
        AffineTransform at = null;
        try
        {
            //double height = 0;
            Rectangle2D bounds = modifier.getTextLayout().getBounds();
            //height = bounds.getHeight();
            at = modifier.getAffineTransform();
            if(at != null)
                bounds = at.createTransformedShape(bounds).getBounds2D();
            modifier.setGlyphPosition(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));

        }
        catch(Exception exc)
        {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
        }
    }

    private static String ShapeToJSONString(ShapeInfo shapeInfo, IPointConversion ipc, Boolean geMap, boolean normalize) {
        StringBuilder JSONed = new StringBuilder();
        /*
        NOTE: Google Earth / KML colors are backwards.
        They are ordered Alpha,Blue,Green,Red, not Red,Green,Blue,Aplha like the rest of the world
         * */
        String fillColor = null;
        String lineColor = null;

        if (shapeInfo.getLineColor() != null)
        {
            lineColor = Integer.toHexString(shapeInfo.getLineColor().getRGB());
            if (geMap) {
                lineColor = JavaRendererUtilities.ARGBtoABGR(lineColor);
            }

        }
        if (shapeInfo.getFillColor() != null)
        {
            fillColor = Integer.toHexString(shapeInfo.getFillColor().getRGB());
            if (geMap) {
                fillColor = JavaRendererUtilities.ARGBtoABGR(fillColor);
            }
        }
        
        BasicStroke stroke = null;
        stroke = (BasicStroke)shapeInfo.getStroke();
        int lineWidth = 4;

        if(stroke != null)
        {
            lineWidth = (int)stroke.getLineWidth();
            //lineWidth++;
            //System.out.println("lineWidth: " + String.valueOf(lineWidth));
        }

        ArrayList shapesArray = shapeInfo.getPolylines();

        for (int i = 0; i < shapesArray.size(); i++) {
            ArrayList shape = (ArrayList) shapesArray.get(i);

            if (fillColor != null) {
                JSONed.append("{\"polygon\":[");
            } else {
                JSONed.append("{\"line\":[");
            }

            //System.out.println("Pixel Coords:");
            for (int j = 0; j < shape.size(); j++) {
                Point2D coord = (Point2D) shape.get(j);
                Point2D geoCoord = ipc.PixelsToGeo(coord);
                //M. Deutch 9-27-11
                if(normalize)
                    geoCoord=NormalizeCoordToGECoord(geoCoord);
                double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
                double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;
                
                //diagnostic M. Deutch 10-18-11
                //set the point as geo so that the 
                //coord.setLocation(longitude, latitude);
                coord=new Point2D.Double(longitude,latitude);
                shape.set(j, coord);
                //end section
                
                JSONed.append("[");
                JSONed.append(longitude);
                JSONed.append(",");
                JSONed.append(latitude);
                JSONed.append("]");

                if (j < (shape.size() - 1)) {
                    JSONed.append(",");
                }
            }

//            JSONed.append("]");
//            JSONed.append(",\"color\":\"");
//            JSONed.append(lineColor);
//            JSONed.append("\"");

            JSONed.append("]");
            if(lineColor != null)
            {
                JSONed.append(",\"lineColor\":\"");
                JSONed.append(lineColor);

                    JSONed.append("\"");
            }
            if(fillColor != null)
            {
                JSONed.append(",\"fillColor\":\"");
                JSONed.append(fillColor);
                JSONed.append("\"");
            }
            
            JSONed.append(",\"lineWidth\":\"");
            JSONed.append(String.valueOf(lineWidth));
            JSONed.append("\"");

            JSONed.append("}");

            if (i < (shapesArray.size() - 1)) {
                JSONed.append(",");
            }
        }

        return JSONed.toString();
    }

    private static String LabelToKMLString(String id, int i, ShapeInfo shapeInfo, IPointConversion ipc, boolean normalize) {
        StringBuilder kml = new StringBuilder();

        Point2D coord = (Point2D) new Point2D.Double(shapeInfo.getGlyphPosition().getX(), shapeInfo.getGlyphPosition().getY());
        Point2D geoCoord = ipc.PixelsToGeo(coord);
        //M. Deutch 9-26-11
        if(normalize)
            geoCoord=NormalizeCoordToGECoord(geoCoord);
        double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
        double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;
        long angle = Math.round(shapeInfo.getModifierStringAngle());

        String text = shapeInfo.getModifierString();

        if (text != null && text.equals("") == false) {
            kml.append("<Placemark id=\"" + id + "_lp" + i + "\">");
            kml.append("<name>" + text + "</name>");
            kml.append("<Style>");
            kml.append("<IconStyle>");
            kml.append("<scale>.7</scale>");
            kml.append("<heading>" + angle + "</heading>");
            kml.append("<Icon>");
            kml.append("<href></href>");
            kml.append("</Icon>");
            kml.append("</IconStyle>");
            kml.append("<LabelStyle>");
            kml.append("<scale>.8</scale>");
            kml.append("</LabelStyle>");
            kml.append("</Style>");
            kml.append("<Point>");
            kml.append("<extrude>1</extrude>");
            kml.append("<altitudeMode>relativeToGround</altitudeMode>");
            kml.append("<coordinates>");
            kml.append(longitude);
            kml.append(",");
            kml.append(latitude);
            kml.append("</coordinates>");
            kml.append("</Point>");
            kml.append("</Placemark>");
        } else {
            return "";
        }

        return kml.toString();
    }

    private static String LabelToJSONString(ShapeInfo shapeInfo, IPointConversion ipc, boolean normalize) {
        StringBuilder JSONed = new StringBuilder();
        /*
        NOTE: Google Earth / KML colors are backwards.
        They are ordered Alpha,Blue,Green,Red, not Red,Green,Blue,Aplha like the rest of the world
         * */
        //String lineColor = Integer.toHexString(shapeInfo.getLineColor().getRGB());
        //lineColor = ARGBtoABGR(lineColor);



        //ArrayList shapesArray = shapeInfo.getPolylines();


        //if(shapesArray.get(i).getClass().getSimpleName().equals("ArrayList") ){

        JSONed.append("{\"label\":");

        //AffineTransform at = shapeInfo.getAffineTransform();
        //Point2D coord = (Point2D)new Point2D.Double(at.getTranslateX(), at.getTranslateY());
        Point2D coord = (Point2D) new Point2D.Double(shapeInfo.getGlyphPosition().getX(), shapeInfo.getGlyphPosition().getY());
        Point2D geoCoord = ipc.PixelsToGeo(coord);
        //M. Deutch 9-27-11
        if(normalize)
            geoCoord=NormalizeCoordToGECoord(geoCoord);
        double latitude = Math.round(geoCoord.getY() * 100000000.0)/100000000.0;
        double longitude = Math.round(geoCoord.getX() * 100000000.0)/100000000.0;
        double angle = shapeInfo.getModifierStringAngle();
        coord.setLocation(longitude, latitude);
        
        //diagnostic M. Deutch 10-18-11
        shapeInfo.setGlyphPosition(coord);


        String text = shapeInfo.getModifierString();

        if (text != null && text.equals("") == false) {
            JSONed.append("[");
            JSONed.append(longitude);
            JSONed.append(",");
            JSONed.append(latitude);
            JSONed.append("]");

            JSONed.append(",\"text\":\"");
            JSONed.append(text);
            JSONed.append("\"");

            JSONed.append(",\"angle\":\"");
            JSONed.append(angle);
            JSONed.append("\"}");
        } else {
            return "";
        }


        return JSONed.toString();
    }
    
    /**
     * Basically renders the symbol with the 2d renderer than pulls out
     * just the label placemarks.  Altitudes are then added so that will place
     * with the 3d symbol they are being added to.
     * @param id
     * @param name
     * @param description
     * @param symbolCode
     * @param controlPoints
     * @param scale
     * @param bbox
     * @param symbolModifiers
     * @param format
     * @param symStd
     * @return 
     */
    public static String getModififerKML(String id,
            String name,
            String description,
            String symbolCode,
            String controlPoints,
            Double scale,
            String bbox,
            String symbolModifiers,
            int format, int symStd)
    {
        String output="";
        List<String> placemarks = new LinkedList<String>();
        
        try
        {
            double maxAlt = 0;
            double minAlt = 0;
            
            output = RenderSymbol(id, name, description, symbolCode, controlPoints, scale, bbox, symbolModifiers, format, symStd);
            int pmiStart = output.indexOf("<Placemark");
            int pmiEnd = 0;
            int curr = 0;
            int count = 0;
            while(pmiStart > 0)
            {
                if(count > 0)
                {
                    pmiEnd = output.indexOf("</Placemark>",pmiStart)+12;
                    placemarks.add(output.substring(pmiStart,pmiEnd));
                    //System.out.println(placemarks.get(count));
                    //end, check for more
                    pmiStart = output.indexOf("<Placemark",pmiEnd-2);
                }
                count++;
            }
            
            //process placemarks if necessary
            List<Double> altitudes = null;
            JSONObject jsonModifiersString = new JSONObject(symbolModifiers);
            JSONObject jsonModifiersArray =
            jsonModifiersString.getJSONObject("modifiers");
            if (jsonModifiersArray.has(ALTITUDE_DEPTH)) {
                JSONArray jsonAltitudeArray = jsonModifiersArray.getJSONArray(ALTITUDE_DEPTH);
                altitudes = new ArrayList<Double>();
                for (int i = 0; i < jsonAltitudeArray.length(); i++) {
                    altitudes.add(jsonAltitudeArray.getDouble(i));
                }
            }
            
            int Xcount = altitudes.size()-1;
            if(Xcount>0)
            {
                maxAlt = altitudes.get(Xcount);
                //cycle through placemarks and add altitude
                String temp;
                for(int j = 0; j<placemarks.size();j++)
                {
                    temp = placemarks.get(j);
                    temp.replace("</coordinates>", "," + String.valueOf(maxAlt) + "</coordinates>");
                    placemarks.set(j, temp);
                }
            }
            
            StringBuilder sb = new StringBuilder();
            for(String pm : placemarks)
            {
                sb.append(pm);
            }
//            System.out.println("placemarks: ");
//            System.out.println(sb.toString());
            return sb.toString();
        }
        catch(Exception exc)
        {
            
        }
        
        return output;
    }

    /**
     * 
     * @param symbolID
     * @param modifiersJSON
     * @param strPoints
     * @return 
     * @deprecated
     */
    public static String getModifierKML2(String symbolID, JSONObject modifiersJSON, String strPoints)
        {
            String[] points = strPoints.split(" ");
            
            JSONArray altitudeDepthJSON = null;
            JSONArray distanceJSON = null;
            JSONArray azimuthJSON = null;
            int altitudeDepthLength = 0;
            int distanceLength = 0;
            int azimuthLength = 0;
            
            String[] point;
            StringBuilder sb = new StringBuilder();
            String name = "<![CDATA[name1<br>name2]]>";
            String description = "<![CDATA[X1<br>X2]]>";//break works in description, not name
            String lat = "0.0";
            String lon = "0.0";
            String alt = "100.0";
            
            try
            {
                System.out.println("generating description KML");
                System.out.println("points: " + points[0]);
                point = points[0].split(",");
                lat = point[0];
                lon = point[1];
                if(point.length<3)
                    alt = "0";
                else
                    alt = point[2];
                
                sb.append("<Placemark>");
                sb.append("<name>" + name + "</name>");
                sb.append("<Style>");
                sb.append("<IconStyle>");
                sb.append("<scale>.7</scale>");
                sb.append("<heading>0</heading>");
                sb.append("<Icon><href/></Icon>");
                sb.append("</IconStyle>");
                sb.append("<LabelStyle>");
                sb.append("<scale>.8</scale>");
                sb.append("</LabelStyle>");
                sb.append("<description>" + description + "</description>");
                sb.append("<Point>");
                //sb.append("<extrude>1</extrude>");
                //sb.append("<altitudeMode>relativeToGround</altitudeMode>");
                sb.append("<coordinates>" + lat + "," + lon + "," + alt + "</coordinates>");
                sb.append("</Point>");
                sb.append("</Placemark>");
            }
            catch(Exception exc)
            {
                System.err.println(exc.getMessage());
                exc.printStackTrace();
            }
            System.out.println("KML segment: ");
            System.out.println(sb.toString());
            return sb.toString();
        }
}
