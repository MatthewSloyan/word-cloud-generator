<%@ include file="includes/header.jsp" %>

<div class="animated bounceInDown" style="font-size:48pt; font-family:arial; color:#990000; font-weight:bold">Web Opinion Visualiser</div>

</p>&nbsp;</p>&nbsp;</p>

<table width="600" cellspacing="0" cellpadding="7" border="0">
	<tr>
		<td valign="top">

			<form bgcolor="white" method="POST" action="doProcess">
				<fieldset>
					<legend><h3>Word Cloud Generator</h3></legend>
				
					<b>Select Options:</b>
					<p>
					
					<p>
					Search Algorithm:
					<select name="optionsSearch">
						<option selected value=1>Best First Search (Most Accurate)</option>
						<option value=2>Recursive Depth First Search (Fastest)</option>
						<option value=3>Beam Search (Accurate and Fast)</option>
					</select>
					<p/>
					
					<p>
					AI Heuristic Type:
					<select name="optionsHeuristic">
						<option selected value=1>Fuzzy Heuristic (Most accurate and fastest)</option>
						<option value=2>Encog Heuristic</option>
						<option value=3>Custom Neural Network Heuristic</option>
					</select>
					<p/>	
			
					<p>
					Scoring Type:
					<select name="optionsScoring">
						<option selected value=1>Frequency</option>
						<option value=2>Levenshtein distance</option>
					</select>
					<p/>	
					
					<p>
					Goal Limit Type:
					<select name="optionsGoal">
						<option value=1>Max Words (Best with Fuzzy)</option>
						<option selected value=2>Max Nodes</option>
					</select>
					<p/>
					
					<p>
					Cloud Word Number:
					<select name="optionsWcNum">
						<option value=10>10</option>
						<option value=20>20</option>
						<option selected value=30>30</option>
					</select>
					<p/>

					<b>Enter Text (Query):</b><br>
					<input name="query" size="100">	
					<p/>

					<center><input type="submit" value="Search & Visualise!"></center>
				</fieldset>							
			</form>	

		</td>
	</tr>
</table>
<%@ include file="includes/footer.jsp" %>

